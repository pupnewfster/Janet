package gg.galaxygaming.janetissuetracker.TeamSpeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import gg.galaxygaming.janetissuetracker.Config;
import gg.galaxygaming.janetissuetracker.Janet;
import gg.galaxygaming.janetissuetracker.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class TeamSpeakMySQL {
    private final int channelAdmin, supporterID, userRooms;
    private String url;
    private Properties properties;
    private ArrayList<Integer> ranks = new ArrayList<>();
    private Thread checkThread;

    public TeamSpeakMySQL() {
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        this.userRooms = config.getIntegerOrDefault("TEAMSPEAK_USER_ROOMS", -1);
        this.channelAdmin = config.getIntegerOrDefault("TEAMSPEAK_CHANEL_ADMIN", -1);
        this.supporterID = config.getIntegerOrDefault("TEAMSPEAK_SUPPORTER", -1);
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") ||
                this.userRooms < 0 || this.channelAdmin < 0 || this.supporterID < 0) {
            System.out.println("[ERROR] Failed to load config for connecting to MySQL Database. (TeamSpeak)");
            return;
        }
        this.url = "jdbc:mysql://" + config.getStringOrDefault("DB_HOST", "127.0.0.1:3306") + '/' + dbName;
        this.properties = new Properties();
        properties.setProperty("user", dbUser);
        properties.setProperty("password", dbPass);
        properties.setProperty("useSSL", "false");
        properties.setProperty("autoReconnect", "true");
        properties.setProperty("useLegacyDatetimeCode", "false");
        properties.setProperty("serverTimezone", "EST");
        indexRanks();
        this.checkThread = new Thread(() -> {
            while (true) {
                if (Janet.DEBUG)
                    System.out.println("[DEBUG] Starting user check (TeamSpeak).");
                checkAll();
                if (Janet.DEBUG)
                    System.out.println("[DEBUG] User check finished (TeamSpeak).");
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        this.checkThread.start();
    }

    public void stop() {
        try {
            this.checkThread.interrupt();
        } catch (Exception ignored) {

        }
    }

    private void indexRanks() {
        try {
            Connection conn = DriverManager.getConnection(this.url, this.properties);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ts_rank_id FROM rank_id_lookup");
            while (rs.next()) {
                int rank = rs.getInt("ts_rank_id");
                if (!this.ranks.contains(rank))//If multiple ranks point to the same one (VIP)
                    this.ranks.add(rank);
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.ranks.add(Janet.getTeamspeak().getVerifiedID());
    }

    private void checkAll() {
        Janet.getTeamspeak().getAsyncApi().getClients().onSuccess(clients -> clients.stream().filter(Client::isRegularClient).forEach(this::check));
    }

    public void check(Client client) {//TODO: cache the website id in case multiple have the same stuff (cache only through single run) this will be more useful for ts
        //TODO create room
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT website_id FROM teamspeak_verified WHERE ts_id = \"" + client.getUniqueIdentifier() + '"');
            ArrayList<Integer> teamspeakRanks = new ArrayList<>();
            String siteID = "";
            if (rs.next()) {
                siteID = rs.getString("website_id");
                rs.close();
                rs = stmt.executeQuery("SELECT member_group_id, mgroup_others FROM core_members WHERE member_id = \"" + siteID + '"');
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
            ArrayList<Integer> oldRanks = new ArrayList<>();
            boolean hasRoom = false, hadRoom = false;
            for (int id : serverGroups) {
                if (!this.ranks.contains(id)) //Rank is not one that Janet modifies
                    continue;
                boolean isSupporter = id == this.supporterID;
                if (teamspeakRanks.contains(id)) {
                    teamspeakRanks.remove(Integer.valueOf(id)); //Already has the rank assigned so remove it from lists of ones
                    if (isSupporter)
                        hasRoom = true;
                } else {
                    oldRanks.add(id);//Add the rank to list to remove
                    if (isSupporter)
                        hadRoom = true;
                }
            }
            TS3ApiAsync api = Janet.getTeamspeak().getAsyncApi();
            int dbID = client.getDatabaseId();
            for (int rID : oldRanks)
                api.removeClientFromServerGroup(rID, dbID);
            for (int rID : teamspeakRanks)
                api.addClientToServerGroup(rID, dbID);
            if (hasRoom) {
                rs = stmt.executeQuery("SELECT ts_room_id FROM verified_rooms WHERE website_id = \"" + siteID + '"');
                if (rs.next())
                    api.setClientChannelGroup(this.channelAdmin, rs.getInt("ts_room_id"), dbID);
                else {
                    String name = client.getNickname();
                    String cname = name + (name.endsWith("s") ? "'" : "'s") + " Room";
                    final HashMap<ChannelProperty, String> properties = new HashMap<>();
                    properties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1");
                    properties.put(ChannelProperty.CPID, Integer.toString(this.userRooms));
                    properties.put(ChannelProperty.CHANNEL_TOPIC, cname);
                    String finalSiteID = siteID;
                    api.createChannel(cname, properties).onSuccess(channelID -> {
                        try (Connection conn2 = DriverManager.getConnection(this.url, this.properties)) {
                            Statement stmt2 = conn2.createStatement();
                            stmt2.execute("REPLACE INTO verified_rooms(website_id,ts_room_id) VALUES(" + finalSiteID + ',' + channelID + ')');
                            stmt2.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        api.moveQuery(Janet.getTeamspeak().getDefaultChannelID()).onSuccess(success -> api.setClientChannelGroup(channelAdmin, channelID, dbID));
                    });
                }
                rs.close();
            } else if (hadRoom) {
                rs = stmt.executeQuery("SELECT ts_room_id FROM verified_rooms WHERE website_id = \"" + siteID + '"');
                //TODO: Check if they currently have a room made, if so make sure they are channel admin
                if (rs.next()) { //Delete room
                    int curRoom = rs.getInt("ts_room_id");
                    rs.close();
                    //TODO if discord room support is added check to see if they have one of those before deleting
                    stmt.execute("DELETE FROM verified_rooms WHERE website_id = \"" + siteID + '"');
                    //TODO maybe do something if it fails/channel does not exist
                    api.deleteChannel(curRoom);
                } else
                    rs.close();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}