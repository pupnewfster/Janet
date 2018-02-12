package gg.galaxygaming.janetissuetracker.Discord;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.permissions.Role;
import gg.galaxygaming.janetissuetracker.Config;
import gg.galaxygaming.janetissuetracker.Janet;
import gg.galaxygaming.janetissuetracker.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class DiscordMySQL {
    private String url;
    private Properties properties;
    private ArrayList<String> ranks = new ArrayList<>();
    private final String verifiedRank, staffRank, seniorRank, donorRank;
    private Thread checkThread;

    public DiscordMySQL() {
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        this.verifiedRank = config.getStringOrDefault("DISCORD_VERIFIED", "verified");
        this.staffRank = config.getStringOrDefault("DISCORD_STAFF", "staff");
        this.seniorRank = config.getStringOrDefault("DISCORD_SENIOR", "senior");
        this.donorRank = config.getStringOrDefault("DISCORD_DONOR", "donor");
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") ||
                this.verifiedRank.equals("verified") || this.staffRank.equals("staff") || this.seniorRank.equals("senior") ||
                this.donorRank.equals("donor")) {
            System.out.println("[ERROR] Failed to load config for connecting to MySQL Database. (Discord)");
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
                    System.out.println("[DEBUG] Starting user check.");
                checkAll();
                if (Janet.DEBUG)
                    System.out.println("[DEBUG] User check finished.");
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
            ResultSet rs = stmt.executeQuery("SELECT discord_rank_id FROM rank_id_lookup");
            while (rs.next()) {
                String rank = rs.getString("discord_rank_id");
                if (!this.ranks.contains(rank))//If multiple ranks point to the same one (VIP)
                    this.ranks.add(rank);
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.ranks.add(this.verifiedRank);
        this.ranks.add(this.staffRank);
        this.ranks.add(this.seniorRank);
        this.ranks.add(this.donorRank);
    }

    private void checkAll() {
        DiscordAPI api = Janet.getDiscord().getApi();
        for (User u : api.getUsers())
            if (!u.isBot() && !u.isYourself())
                check(u);
    }

    public void check(User user) {//TODO: cache the website id in case multiple have the same stuff (cache only through single run) this will be more useful for ts
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT website_id FROM discord_verified WHERE discord_id = \"" + user.getId() + '"');
            ArrayList<String> discordRanks = new ArrayList<>();
            if (rs.next()) {
                String siteID = rs.getString("website_id");
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
                    rs = stmt.executeQuery("SELECT discord_rank_id FROM rank_id_lookup WHERE " + query);
                    boolean isStaff = false, isSenior = false, isDonor = false;
                    Statement stmt2 = conn.createStatement();
                    while (rs.next()) {
                        String id = rs.getString("discord_rank_id");
                        if (id.equals("NULL"))
                            continue;
                        discordRanks.add(id);
                        ResultSet rs2 = stmt2.executeQuery("SELECT staff, senior, donor FROM discord_is_staff WHERE discord_rank_id = " + id);
                        if (rs2.next()) {
                            if (!isDonor && rs2.getBoolean("donor"))
                                isDonor = true;
                            if (!isSenior) {
                                if (rs2.getBoolean("senior")) {
                                    isStaff = true;
                                    isSenior = true;
                                } else if (!isStaff && rs2.getBoolean("staff"))
                                    isStaff = true;
                            }
                            rs2.close();
                        }
                    }
                    stmt2.close();
                    rs.close();
                    discordRanks.add(this.verifiedRank);
                    if (isStaff)
                        discordRanks.add(this.staffRank);
                    if (isSenior)
                        discordRanks.add(this.seniorRank);
                    if (isDonor)
                        discordRanks.add(this.donorRank);
                }
            }
            Server server = Janet.getDiscord().getServer();
            Collection<Role> roles = user.getRoles(server);
            boolean changed = false;
            ArrayList<String> newRanks = new ArrayList<>();
            for (Role r : roles) {
                String id = r.getId();
                if (!this.ranks.contains(id)) //Rank is not one that gets set by janet
                    newRanks.add(id);
                else if (discordRanks.contains(id)) {
                    newRanks.add(id);
                    discordRanks.remove(id); //Remove from list of ones they have
                } else
                    changed = true;
            }
            if (!discordRanks.isEmpty()) {
                newRanks.addAll(discordRanks);
                changed = true;
            }
            if (changed) {
                Role[] newRoles = new Role[newRanks.size()];
                for (int i = 0; i < newRanks.size(); i++)
                    newRoles[i] = server.getRoleById(newRanks.get(i));
                server.updateRoles(user, newRoles);
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}