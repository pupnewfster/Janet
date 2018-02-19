package gg.galaxygaming.janet.TeamSpeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.api.AbstractMySQL;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.ranks.add(Janet.getTeamspeak().getVerifiedID());
    }

    protected void checkAll() {//TODO: Does this need better stop support added
        Janet.getTeamspeak().getAsyncApi().getClients().onSuccess(clients -> clients.stream().filter(Client::isRegularClient).forEach(c -> {
            if (!stop)
                check(c);
        }));
    }

    /**
     * Checks to see if a {@link Client} is authenticated and if so give them their ranks.
     * @param client The {@link Client} to check.
     */
    public void check(@Nonnull Client client) {//TODO: cache the website id in case multiple have the same stuff (cache only through single run) this will be more useful for ts
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT website_id FROM teamspeak_verified WHERE ts_id = \"" + client.getUniqueIdentifier() + '"');
            List<Integer> teamspeakRanks = new ArrayList<>();
            String siteID = null;
            if (rs.next()) {
                siteID = rs.getString("website_id");
                rs.close();
                rs = stmt.executeQuery("SELECT member_group_id, mgroup_others FROM core_members WHERE member_id = " + siteID);
                if (rs.next()) {
                    int primary = rs.getInt("member_group_id");
                    String secondary = rs.getString("mgroup_others");
                    rs.close();
                    String[] secondaries = secondary.split(",");
                    StringBuilder sbGroups = new StringBuilder(Integer.toString(primary));
                    int gCount = 1;
                    for (String s : secondaries)
                        if (Utils.legalInt(s)) {
                            sbGroups.append(',').append(s);
                            gCount++;
                        }
                    String groups = sbGroups.toString().trim();
                    String query = gCount == 1 ? "site_rank_id = " + groups : "site_rank_id IN (" + groups + ')';
                    rs = stmt.executeQuery("SELECT ts_rank_id FROM rank_id_lookup WHERE " + query);
                    while (rs.next()) {
                        int rank = rs.getInt("ts_rank_id");
                        if (rank >= 0)
                            teamspeakRanks.add(rank);
                    }
                    rs.close();
                    teamspeakRanks.add(Janet.getTeamspeak().getVerifiedID());
                } else
                    rs.close();
            } else
                rs.close();
            int[] serverGroups = client.getServerGroups();
            List<Integer> oldRanks = new ArrayList<>();
            boolean hasRoom = teamspeakRanks.contains(this.supporterID), hadRoom = false;
            for (int id : serverGroups) {
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
            int dbID = client.getDatabaseId();
            for (int rID : oldRanks)
                api.removeClientFromServerGroup(rID, dbID);
            for (int rID : teamspeakRanks)
                api.addClientToServerGroup(rID, dbID);
            int curRoom = -1;
            long discord = -1;
            if (siteID != null) {
                rs = stmt.executeQuery("SELECT * FROM verified_rooms WHERE website_id = " + siteID);
                if (rs.next()) {
                    curRoom = rs.getInt("ts_room_id");
                    discord = rs.getLong("discord_room_id");
                }
                rs.close();
            }
            if (hasRoom || hadRoom || curRoom >= 0) {
                if (hasRoom) {
                    if (curRoom >= 0) {
                        if (client.getChannelGroupId() != this.channelAdmin)//If they already have channel admin do not bother setting it on them again
                            api.setClientChannelGroup(this.channelAdmin, curRoom, dbID);
                    } else {
                        String name = client.getNickname();
                        String cname = name + (name.endsWith("s") ? "'" : "'s") + " Room";
                        final Map<ChannelProperty, String> properties = new HashMap<>();
                        properties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1");
                        properties.put(ChannelProperty.CPID, Integer.toString(this.userRooms));
                        properties.put(ChannelProperty.CHANNEL_TOPIC, cname);
                        String finalSiteID = siteID;
                        api.createChannel(cname, properties).onSuccess(channelID -> {
                            try (Connection conn2 = DriverManager.getConnection(this.url, this.properties)) {
                                Statement stmt2 = conn2.createStatement();
                                stmt2.execute("INSERT INTO verified_rooms(website_id,discord_room_id,ts_room_id) VALUES(" + finalSiteID + ",-1," + channelID +
                                        ") ON DUPLICATE KEY UPDATE ts_room_id = " + channelID);
                                stmt2.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            api.moveQuery(Janet.getTeamspeak().getDefaultChannelID()).onSuccess(success -> api.setClientChannelGroup(channelAdmin, channelID, dbID));
                        });
                    }
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }
}