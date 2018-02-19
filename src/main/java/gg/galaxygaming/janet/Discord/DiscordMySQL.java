package gg.galaxygaming.janet.Discord;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.channels.ServerVoiceChannelBuilder;
import de.btobastian.javacord.entities.channels.ServerVoiceChannelUpdater;
import de.btobastian.javacord.entities.permissions.*;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.api.AbstractMySQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.MySQL} to handle all MySQL
 * interactions with the tables pertaining to Discord.
 */
public class DiscordMySQL extends AbstractMySQL {
    private ArrayList<Long> ranks = new ArrayList<>();
    private long verifiedRank, staffRank, seniorRank, donorRank, supporterID, userRooms;

    public DiscordMySQL() {
        super();
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        this.verifiedRank = config.getLongOrDefault("DISCORD_VERIFIED", -1);
        this.staffRank = config.getLongOrDefault("DISCORD_STAFF", -1);
        this.seniorRank = config.getLongOrDefault("DISCORD_SENIOR", -1);
        this.donorRank = config.getLongOrDefault("DISCORD_DONOR", -1);
        this.supporterID = config.getLongOrDefault("DISCORD_SUPPORTER", -1);
        this.userRooms = config.getLongOrDefault("DISCORD_USER_ROOMS", -1);
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") || this.verifiedRank < 0 || this.staffRank < 0 ||
                this.seniorRank < 0 || this.donorRank < 0 || supporterID < 0 || this.userRooms < 0) {
            Janet.getLogger().error("Failed to load config for connecting to MySQL Database. (Discord)");
            return;
        }
        this.url = "jdbc:mysql://" + config.getStringOrDefault("DB_HOST", "127.0.0.1:3306") + '/' + dbName;
        properties.setProperty("user", dbUser);
        properties.setProperty("password", dbPass);
        indexRanks();
        this.service = "Discord";
        this.checkThread.start();
    }

    /**
     * Index all the ranks that {@link Janet} can assign.
     */
    private void indexRanks() {
        try {
            Connection conn = DriverManager.getConnection(this.url, this.properties);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT discord_rank_id FROM rank_id_lookup");
            while (rs.next()) {
                long rank = rs.getLong("discord_rank_id");
                if (rank >= 0 && !this.ranks.contains(rank))//If multiple ranks point to the same one (VIP)
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

    protected void checkAll() {
        DiscordApi api = Janet.getDiscord().getApi();
        for (User u : api.getCachedUsers())
            if (!u.isBot() && !u.isYourself())
                check(u);
    }

    /**
     * Checks to see if a {@link User} is authenticated and if so give them their ranks.
     * @param user The {@link User} to check.
     */
    private void check(User user) {//TODO: cache the website id in case multiple have the same stuff (cache only through single run) this will be more useful for ts
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT website_id FROM discord_verified WHERE discord_id = " + user.getId());
            ArrayList<Long> discordRanks = new ArrayList<>();
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
                    rs = stmt.executeQuery("SELECT discord_rank_id FROM rank_id_lookup WHERE " + query);
                    boolean isStaff = false, isSenior = false, isDonor = false;
                    Statement stmt2 = conn.createStatement();
                    while (rs.next()) {
                        long id = rs.getLong("discord_rank_id");
                        if (id < 0)
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
                        } else
                            rs2.close();
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
                } else
                    rs.close();
            } else
                rs.close();
            Server server = Janet.getDiscord().getServer();
            Collection<Role> roles = user.getRoles(server);
            boolean changed = false;
            ArrayList<Long> newRanks = new ArrayList<>();
            boolean hadRoom = discordRanks.contains(this.supporterID);
            for (Role r : roles) {
                long id = r.getId();
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
            boolean hasRoom = newRanks.contains(this.supporterID);
            if (changed) {
                ArrayList<Role> newRoles = new ArrayList<>();
                for (Long newRank : newRanks)
                    server.getRoleById(newRank).ifPresent(newRoles::add);
                server.updateRoles(user, newRoles);
            }
            int ts = -1;
            long discord = -1;
            if (siteID != null) {
                rs = stmt.executeQuery("SELECT * FROM verified_rooms WHERE website_id = " + siteID);
                if (rs.next()) {
                    ts = rs.getInt("ts_room_id");
                    discord = rs.getLong("discord_room_id");
                }
                rs.close();
            }
            if (hasRoom || hadRoom || discord >= 0) {
                int finalTs = ts;
                String finalSiteID = siteID;
                if (hasRoom) {
                    if (!hadRoom || discord < 0) {//Create it for them
                        String name = user.getDisplayName(server);
                        String fname = name + (name.endsWith("s") ? "'" : "'s") + " Room";
                        server.getChannelCategoryById(userRooms).ifPresent(c -> new ServerVoiceChannelBuilder(server).setName(fname).setCategory(c).create().thenAccept(vc -> {
                            try (Connection conn2 = DriverManager.getConnection(this.url, this.properties)) {
                                Statement stmt2 = conn2.createStatement();
                                stmt2.execute("REPLACE INTO verified_rooms(website_id,discord_room_id,ts_room_id) VALUES(" + finalSiteID + ',' + vc.getId() + ',' + finalTs + ')');
                                stmt2.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            ArrayList<Permissions> perms = new ArrayList<>();
                            perms.add(new PermissionsBuilder().setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED).build());
                            perms.add(new PermissionsBuilder().setState(PermissionType.VOICE_MOVE_MEMBERS, PermissionState.ALLOWED).build());
                            //TODO maybe add a way to lock room?
                            ServerVoiceChannelUpdater vcUpdater = vc.getUpdater();
                            perms.forEach(p -> vcUpdater.addPermissionOverwrite(user, p));
                            vcUpdater.update();
                        }));
                    }
                } else {//hadRoom, Delete it because they no longer should have it
                    server.getVoiceChannelById(discord).ifPresent(vc -> vc.delete().thenAccept(v -> {
                        //If successfully deleted remove it from the table
                        try (Connection conn2 = DriverManager.getConnection(this.url, this.properties)) {
                            Statement stmt2 = conn2.createStatement();
                            if (finalTs < 0)
                                stmt2.execute("DELETE FROM verified_rooms WHERE website_id = " + finalSiteID);
                            else
                                stmt2.executeUpdate("UPDATE verified_rooms SET discord_room_id = -1 WHERE website_id =" + finalSiteID);
                            stmt2.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }));
                }
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the highest {@link Rank} that is contained by the list of {@link Role}s.
     * @param roles The list of roles to calculate the highest {@link Rank} from.
     * @return The highest {@link Rank} that is contained by the list of {@link Role}s.
     */
    public Rank getRankPower(List<Role> roles) {
        Rank r = Rank.MEMBER;
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            for (Role role : roles) {
                ResultSet rs = stmt.executeQuery("SELECT rank_power FROM rank_id_lookup WHERE discord_rank_id = " + role.getId());
                if (rs.next()) {
                    r = Rank.fromPower(rs.getInt("rank_power"));
                    rs.close();
                    break;
                }
                rs.close();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }
}