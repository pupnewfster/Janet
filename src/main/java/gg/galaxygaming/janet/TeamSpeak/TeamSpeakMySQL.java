package gg.galaxygaming.janet.TeamSpeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Forums.ForumMySQL;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.api.AbstractMySQL;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.MySQL} to handle all MySQL
 * interactions with the tables pertaining to TeamSpeak.
 */
public class TeamSpeakMySQL extends AbstractMySQL {
    private final int channelAdmin, supporterID, userRooms;
    private final List<Integer> ranks = new ArrayList<>();

    public TeamSpeakMySQL() {
        super();
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        this.userRooms = config.getIntegerOrDefault("TEAMSPEAK_USER_ROOMS", -1);
        this.channelAdmin = config.getIntegerOrDefault("TEAMSPEAK_CHANEL_ADMIN", -1);
        this.supporterID = config.getIntegerOrDefault("TEAMSPEAK_SUPPORTER", -1);
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") ||
                this.userRooms < 0 || this.channelAdmin < 0 || this.supporterID < 0) {
            Janet.getLogger().error("Failed to load config for connecting to MySQL Database. (TeamSpeak)");
            return;
        }
        this.url = "jdbc:mysql://" + config.getStringOrDefault("DB_HOST", "127.0.0.1:3306") + '/' + dbName;
        properties.setProperty("user", dbUser);
        properties.setProperty("password", dbPass);
        indexRanks();
        this.service = "TeamSpeak";
        this.checkThread.start();
    }

    /**
     * Index all the ranks that {@link Janet} can assign.
     */
    private void indexRanks() {
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ts_rank_id FROM rank_id_lookup");
            while (rs.next()) {
                int rank = rs.getInt("ts_rank_id");
                if (!this.ranks.contains(rank))//If multiple ranks point to the same one (VIP)
                    this.ranks.add(rank);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.ranks.add(Janet.getTeamspeak().getVerifiedID());
    }

    protected void checkAll() {
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM teamspeak_verified");
            while (rs.next()) {
                if (stop)
                    break;
                String siteID = rs.getString("website_id");
                String tsID = rs.getString("ts_id");
                if (siteID != null && tsID != null)
                    Janet.getTeamspeak().getAsyncApi().getDatabaseClientByUId(tsID).onSuccess(dbInfo -> {
                        if (dbInfo != null)
                            check(dbInfo, siteID);
                    });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks to see if a {@link DatabaseClientInfo} is authenticated and if so give them their ranks.
     * @param dbInfo The {@link DatabaseClientInfo} of the client to check.
     * @param siteID The website id of the given {@link DatabaseClientInfo}.
     */
    public void check(@Nonnull DatabaseClientInfo dbInfo, @Nonnull String siteID) {//TODO: cache the website id in case multiple have the same stuff (cache only through single run) this will be more useful for ts
        List<Integer> teamspeakRanks = new ArrayList<>();
        teamspeakRanks.add(Janet.getTeamspeak().getVerifiedID());
        String query = ((ForumMySQL) Janet.getForums().getMySQL()).getRankQuery(siteID);
        if (query != null)
            try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT ts_rank_id FROM rank_id_lookup WHERE " + query);
                while (rs.next()) {
                    int rank = rs.getInt("ts_rank_id");
                    if (rank >= 0)
                        teamspeakRanks.add(rank);
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        int dbID = dbInfo.getDatabaseId();
        Janet.getTeamspeak().getAsyncApi().getServerGroupsByClientId(dbID).onSuccess(serverGroups -> {
            List<Integer> oldRanks = new ArrayList<>();
            List<Integer> groupIDs = new ArrayList<>();
            serverGroups.forEach(g -> groupIDs.add(g.getId()));
            boolean hasRoom = teamspeakRanks.contains(this.supporterID) || getRankPower(teamspeakRanks).hasRank(Rank.MANAGER),
                    hadRoom = getRankPower(groupIDs).hasRank(Rank.MANAGER);
            for (int id : groupIDs) {
                if (!this.ranks.contains(id)) //Rank is not one that Janet modifies
                    continue;
                if (teamspeakRanks.contains(id))
                    teamspeakRanks.remove(Integer.valueOf(id)); //Already has the rank assigned so remove it from lists of ones
                else {
                    oldRanks.add(id);//Add the rank to list to remove
                    if (id == this.supporterID)
                        hadRoom = true;
                }
            }
            TS3ApiAsync api = Janet.getTeamspeak().getAsyncApi();
            for (int rID : oldRanks)
                api.removeClientFromServerGroup(rID, dbID);
            for (int rID : teamspeakRanks)
                api.addClientToServerGroup(rID, dbID);
            checkRoom(dbInfo, siteID, hasRoom, hadRoom);
        });
    }

    /**
     * Checks to see if the given {@link DatabaseClientInfo} should have a room, and if so create it for them.
     * @param dbInfo  The {@link DatabaseClientInfo} to check.
     * @param siteID  The siteID of the {@link DatabaseClientInfo}.
     * @param hasRoom True if the {@link DatabaseClientInfo} should have a room, false otherwise.
     * @param hadRoom True if the user used to have a room and the room should be deleted, false otherwise.
     */
    private void checkRoom(@Nonnull DatabaseClientInfo dbInfo, @Nonnull String siteID, boolean hasRoom, boolean hadRoom) {
        int curRoom = -1;
        long discord = -1;
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM verified_rooms WHERE website_id = " + siteID);
            if (rs.next()) {
                curRoom = rs.getInt("ts_room_id");
                discord = rs.getLong("discord_room_id");
            }
            rs.close();
            if (hasRoom || hadRoom || curRoom >= 0) {
                TS3ApiAsync api = Janet.getTeamspeak().getAsyncApi();
                if (hasRoom) {
                    int finalCurRoom = curRoom;
                    Janet.getTeamspeak().getAsyncApi().getClientByUId(dbInfo.getUniqueIdentifier()).onSuccess(client -> {
                        if (finalCurRoom >= 0) {
                            if (client == null || client.getChannelGroupId() != this.channelAdmin)//If they already have channel admin do not bother setting it on them again
                                api.setClientChannelGroup(this.channelAdmin, finalCurRoom, dbInfo.getDatabaseId());
                        } else if (client != null) {
                            String name = client.getNickname();
                            String cname = name + (name.endsWith("s") ? "'" : "'s") + " Room";
                            final Map<ChannelProperty, String> properties = new HashMap<>();
                            properties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1");
                            properties.put(ChannelProperty.CPID, Integer.toString(this.userRooms));
                            properties.put(ChannelProperty.CHANNEL_TOPIC, cname);
                            api.createChannel(cname, properties).onSuccess(channelID -> {
                                try (Connection conn2 = DriverManager.getConnection(this.url, this.properties)) {
                                    Statement stmt2 = conn2.createStatement();
                                    stmt2.execute("INSERT INTO verified_rooms(website_id,discord_room_id,ts_room_id) VALUES(" + siteID + ",-1," + channelID +
                                            ") ON DUPLICATE KEY UPDATE ts_room_id = " + channelID);
                                    stmt2.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                api.moveQuery(Janet.getTeamspeak().getDefaultChannelID()).onSuccess(success -> api.setClientChannelGroup(channelAdmin, channelID, dbInfo.getDatabaseId()));
                            });
                        }
                    });
                } else {//hadRoom
                    //Delete room
                    if (discord < 0)
                        stmt.execute("DELETE FROM verified_rooms WHERE website_id = " + siteID);
                    else
                        stmt.executeUpdate("UPDATE verified_rooms SET ts_room_id = -1 WHERE website_id =" + siteID);
                    //TODO maybe do something if it fails/channel does not exist
                    api.deleteChannel(curRoom);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the highest {@link Rank} that is contained by the list of serverGroups.
     * @param serverGroups The list of server groups to calculate the highest {@link Rank} from.
     * @return The highest {@link Rank} that is contained by the list of serverGroups.
     */
    @Nonnull
    public Rank getRankPower(int[] serverGroups) {
        return getRankPower(Arrays.stream(serverGroups).boxed().collect(Collectors.toList()));
    }

    /**
     * Retrieves the highest {@link Rank} that is contained by the list of serverGroups.
     * @param serverGroups The list of server groups to calculate the highest {@link Rank} from.
     * @return The highest {@link Rank} that is contained by the list of serverGroups.
     */
    @Nonnull
    public Rank getRankPower(List<Integer> serverGroups) {
        Rank r = Rank.MEMBER;
        StringBuilder sbGroups = new StringBuilder();
        int gCount = 0;
        for (int sg : serverGroups) {
            sbGroups.append(',').append(sg);
            gCount++;
        }
        if (gCount == 0)
            return r;
        String groups = sbGroups.toString().substring(1).trim();
        String query = gCount == 1 ? "ts_rank_id = " + groups : "ts_rank_id IN (" + groups + ')';
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT rank_power FROM rank_id_lookup WHERE " + query + " ORDER BY rank_power DESC");
            if (rs.next())
                r = Rank.fromPower(rs.getInt("rank_power"));
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return r;
    }
}