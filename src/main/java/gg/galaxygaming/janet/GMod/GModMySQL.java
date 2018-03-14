package gg.galaxygaming.janet.GMod;

import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.api.AbstractMySQL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.*;
import java.util.*;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.MySQL} to handle all MySQL
 * interactions with the tables pertaining to GMod.
 */
public class GModMySQL extends AbstractMySQL {
    private String gmodURL;
    private Properties gmodProperties;

    public GModMySQL() {
        super("GMod");
        Config config = Janet.getConfig();
        String dbName = config.getOrDefault("DB_NAME", "database");
        String dbUser = config.getOrDefault("DB_USER", "user");
        String dbPass = config.getOrDefault("DB_PASSWORD", "password");
        String gmodName = config.getOrDefault("GMOD_DB_NAME", "database");
        String gmodUser = config.getOrDefault("GMOD_DB_USER", "user");
        String gmodPass = config.getOrDefault("GMOD_DB_PASSWORD", "password");
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") || gmodPass.equals("password") ||
                gmodUser.equals("user") || gmodName.equals("database")) {
            Janet.getLogger().error("Failed to load config for connecting to MySQL Database. (" + this.service + ')');
            return;
        }
        String host = config.getOrDefault("DB_HOST", "127.0.0.1:3306");
        this.url = "jdbc:mysql://" + host + '/' + dbName;
        this.gmodURL = "jdbc:mysql://" + host + '/' + gmodName;
        this.properties.setProperty("user", dbUser);
        this.properties.setProperty("password", dbPass);
        this.gmodProperties = new Properties();
        this.gmodProperties.setProperty("user", gmodUser);
        this.gmodProperties.setProperty("password", gmodPass);
        this.gmodProperties.setProperty("useSSL", "false");
        this.gmodProperties.setProperty("autoReconnect", "true");
        this.gmodProperties.setProperty("useLegacyDatetimeCode", "false");
        this.gmodProperties.setProperty("serverTimezone", "EST");
        this.checkThread.start();
    }

    /**
     * Gets the database properties for the GMod MySQL database.
     * @return The database properties for the GMod MySQL database.
     */
    @Nullable
    public Properties getGModProperties() {
        return this.gmodProperties;
    }

    /**
     * Gets the database url for the GMod MySQL database.
     * @return The database url for the GMod MySQL database.
     */
    @Nullable
    public String getGModURL() {
        return this.gmodURL;
    }

    protected void checkAll() {
        Set<String> servers = getServers(); //Server list changes during a current check do not matter, just cache the value
        if (servers.isEmpty())
            return;
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT member_group_id, mgroup_others, steamid FROM core_members WHERE steamid IS NOT NULL");
            while (rs.next()) {
                if (stop)
                    break;
                String steamid = rs.getString("steamid");
                if (steamid.isEmpty() || steamid.equals("0"))
                    continue;
                int primary = rs.getInt("member_group_id");
                String secondary = rs.getString("mgroup_others");
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
                ResultSet rs2 = stmt2.executeQuery("SELECT gmod_rank_id, rank_power FROM rank_id_lookup WHERE " + query);
                List<Rank> gmodRanks = new ArrayList<>();
                while (rs2.next()) {
                    String id = rs2.getString("gmod_rank_id");
                    if (id != null)
                        gmodRanks.add(new Rank(id, rs2.getInt("rank_power")));
                }
                rs2.close();
                check(steamid, gmodRanks, servers);
            }
            rs.close();
            stmt.close();
            stmt2.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gives the user with the given steam64 id any ranks that they have on the GMod servers.
     * @param steamid   The steam64 id to check.
     * @param gmodRanks The ranks to set.
     * @param servers   The servers we are tracking.
     */
    private void check(@Nonnull String steamid, @Nonnull List<Rank> gmodRanks, @Nonnull Set<String> servers) {
        Map<String, Rank> serverRanks = new HashMap<>();
        for (Rank rank : gmodRanks) {
            String rName = rank.getID();
            if (rName.contains("_")) {
                String[] rankInfo = rName.split("_");
                String server = rankInfo[0] + "_rank", r = rankInfo[1];
                if (!serverRanks.containsKey(server) || rank.getPower() > serverRanks.get(server).getPower())
                    serverRanks.put(server, new Rank(r, rank.getPower()));
            } else
                for (String server : servers)
                    if (!serverRanks.containsKey(server) || rank.getPower() > serverRanks.get(server).getPower())
                        serverRanks.put(server, rank);
        }
        try (Connection conn = DriverManager.getConnection(this.gmodURL, this.gmodProperties)) {//Write to a database so janet gmod can read them
            Statement stmt = conn.createStatement();
            boolean update = false;
            ResultSet rs = stmt.executeQuery("SELECT * FROM gmod_ranks WHERE steamid = \"" + steamid + '"');
            if (rs.next()) {
                if (serverRanks.isEmpty())
                    stmt.execute("DELETE FROM gmod_ranks WHERE steamid = \"" + steamid + '"');
                else
                    for (String server : servers)
                        if (!serverRanks.containsKey(server) || !serverRanks.get(server).getID().equals(rs.getString(server)))
                            update = true;
            } else if (!serverRanks.isEmpty())
                update = true;
            rs.close();
            if (update) {
                StringBuilder columns = new StringBuilder("steamid");
                StringBuilder values = new StringBuilder('"' + steamid + '"');
                StringBuilder valuesUpdate = new StringBuilder();
                for (String server : servers) {
                    columns.append(',').append(server);
                    if (serverRanks.containsKey(server)) {
                        Rank value = serverRanks.get(server);
                        values.append(",\"").append(value.getID()).append('"');
                        valuesUpdate.append(',').append(server).append(" = \"").append(value.getID()).append('"');
                    } else {
                        values.append(",\"NULL\"");
                        valuesUpdate.append(',').append(server).append(" = \"NULL\"");
                    }
                }
                stmt.execute("INSERT INTO gmod_ranks(" + columns.toString() + ") VALUES(" + values.toString() + ") ON DUPLICATE KEY UPDATE "
                        + valuesUpdate.toString().substring(1));
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets all the GMod servers that we know the ranks of.
     * @return A {@link Set} containing all the GMod servers that we know the ranks of.
     */
    @Nonnull
    private Set<String> getServers() {
        Set<String> servers = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(this.gmodURL, this.gmodProperties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM gmod_ranks");
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            for (int i = 1; i <= count; i++) {
                String name = meta.getColumnName(i);
                if (name.contains("_"))
                    servers.add(name);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return servers;
    }

    /**
     * A wrapper that stores the gmod rank name and the power associated with it.
     */
    public class Rank {
        private final String id;
        private final int power;

        public Rank(@Nonnull String id, int power) {
            this.id = id;
            this.power = power;
        }

        /**
         * Retrieves the name of this gmod rank.
         * @return The name of this gmod rank.
         */
        @Nonnull
        public String getID() {
            return this.id;
        }

        /**
         * Retrieves the power this gmod rank has.
         * @return The power of this gmod rank.
         */
        public int getPower() {
            return this.power;
        }
    }
}