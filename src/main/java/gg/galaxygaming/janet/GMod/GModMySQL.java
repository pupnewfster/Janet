package gg.galaxygaming.janet.GMod;

import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.base.AbstractMySQL;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public class GModMySQL extends AbstractMySQL {
    private final File urlFile;
    private String gmodURL;
    private Properties gmodProperties;

    public GModMySQL() {
        super();
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        String gmodName = config.getStringOrDefault("GMOD_DB_NAME", "database");
        String gmodUser = config.getStringOrDefault("GMOD_DB_USER", "user");
        String gmodPass = config.getStringOrDefault("GMOD_DB_PASSWORD", "password");
        this.urlFile = new File(config.getStringOrDefault("GMOD_STEAMID_FILE", "steamidfile"));
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") || gmodPass.equals("password") ||
                gmodUser.equals("user") || gmodName.equals("database") || !this.urlFile.exists()) {
            System.out.println("[ERROR] Failed to load config for connecting to MySQL Database. (GMod)");
            return;
        }
        String host = config.getStringOrDefault("DB_HOST", "127.0.0.1:3306");
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
        this.service = "GMod";
        this.checkThread.start();
    }

    protected void checkAll() {
        ArrayList<String> urls = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(this.urlFile))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (!line.equals(""))
                    urls.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String u : urls)
            try {
                URL url = new URL(u);
                StringBuilder response;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String inputLine;
                    response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null)
                        response.append(inputLine);
                }
                String r = response.toString().trim();
                if (!r.isEmpty()) {
                    String[] steamids = r.substring(0, r.length() - 1).split(";");
                    for (String steamid : steamids)
                        if (!steamid.isEmpty())
                            check(steamid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private void check(String steamid) {
        ArrayList<Rank> gmodRanks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT member_group_id, mgroup_others FROM core_members WHERE steamid = \"" + steamid + '"');
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
                rs = stmt.executeQuery("SELECT gmod_rank_id, rank_power FROM rank_id_lookup WHERE " + query);
                while (rs.next()) {
                    String id = rs.getString("gmod_rank_id");
                    if (id.equals("NULL"))
                        continue;
                    gmodRanks.add(new Rank(id, rs.getInt("rank_power")));
                }
                rs.close();
            } else {//No one has that steam id linked with their account
                rs.close();
                stmt.close();
                return;
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (Connection conn = DriverManager.getConnection(this.gmodURL, this.gmodProperties)) {
            Statement stmt = conn.createStatement();
            HashMap<String, Rank> serverRanks = new HashMap<>();
            HashSet<String> servers = new HashSet<>();
            ResultSet rs = stmt.executeQuery("SELECT * FROM gmod_ranks");
            ResultSetMetaData rsmd = rs.getMetaData();
            int count = rsmd.getColumnCount();
            for (int i = 1; i <= count; i++) {
                String name = rsmd.getColumnName(i);
                if (name.contains("_"))
                    servers.add(name);
            }
            rs.close();
            for (Rank rank : gmodRanks) {
                String rName = rank.getID();
                if (rName.contains("_")) {
                    String[] rankInfo = rName.split("_");
                    String server = rankInfo[0] + "_rank", r = rankInfo[1];
                    if (serverRanks.containsKey(server)) {
                        if (rank.getPower() > serverRanks.get(server).getPower())
                            serverRanks.put(server, new Rank(r, rank.getPower()));
                    } else
                        serverRanks.put(server, new Rank(r, rank.getPower()));
                } else
                    for (String server : servers)
                        if (serverRanks.containsKey(server)) {
                            if (rank.getPower() > serverRanks.get(server).getPower())
                                serverRanks.put(server, rank);
                        } else
                            serverRanks.put(server, rank);
            }
            //Write to a database so janet gmod can read them
            boolean update = false;
            rs = stmt.executeQuery("SELECT * FROM gmod_ranks WHERE steamid = \"" + steamid + '"');
            if (rs.next()) {
                if (serverRanks.isEmpty())
                    stmt.execute("DELETE FROM gmod_ranks WHERE steamid = \"" + steamid + '"');
                else //If one of the values is not already set, then keep the old value for it
                    for (String server : servers)
                        if (!serverRanks.containsKey(server)) {
                            serverRanks.put(server, new Rank(rs.getString(server), 0));//Power may not be 0 but it does not matter. We are passed where that is checked
                            update = true;
                        }
            } else
                update = true;
            rs.close();
            if (update) {
                StringBuilder columns = new StringBuilder("steamid");
                StringBuilder values = new StringBuilder('"' + steamid + '"');
                for (String server : servers) {
                    Rank value = serverRanks.get(server);
                    columns.append(',').append(server);
                    if (value == null)
                        values.append(",\"NULL\"");
                    else
                        values.append(",\"").append(value.getID()).append('"');
                }
                stmt.execute("REPLACE INTO gmod_ranks(" + columns.toString() + ") VALUES(" + values.toString() + ')');
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class Rank {
        private String id;
        private int power;

        public Rank(String id, int power) {
            this.id = id;
            this.power = power;
        }

        public String getID() {
            return this.id;
        }

        public int getPower() {
            return this.power;
        }
    }
}