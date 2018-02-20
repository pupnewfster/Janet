package gg.galaxygaming.janet.Forums.donations;

import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Forums.ForumMySQL;
import gg.galaxygaming.janet.GMod.GModMySQL;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.api.AbstractMySQL;

import java.sql.*;
import java.util.Calendar;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.MySQL} to handle all MySQL
 * interactions with the tables pertaining to Donations.
 */
public class DonationMySQL extends AbstractMySQL {
    public DonationMySQL() {
        super();
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user")) {
            Janet.getLogger().error("Failed to load config for connecting to MySQL Database. (Donations)");
            return;
        }
        this.url = "jdbc:mysql://" + config.getStringOrDefault("DB_HOST", "127.0.0.1:3306") + '/' + dbName;
        properties.setProperty("user", dbUser);
        properties.setProperty("password", dbPass);
        this.service = "Donations";
        this.checkThread.start();
    }

    @Override
    protected void checkAll() {
        checkRanks();
        checkPS2();
    }

    /**
     * Checks PointShop 2 donations.
     */
    private void checkPS2() {
        GModMySQL gmodSQL = (GModMySQL) Janet.getGMod().getMySQL();
        if (gmodSQL.getGModURL() == null || gmodSQL.getGModProperties() == null)
            return;
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM ps2_info");
            while (rs.next()) {
                if (stop)
                    break;
                int points = rs.getInt("points");
                String server = rs.getString("server");
                if (points == 0)
                    continue;
                int siteID = rs.getInt("site_id");
                String steamid = null;
                ResultSet rs2 = stmt2.executeQuery("SELECT steamid FROM core_members WHERE member_id = " + siteID);
                if (rs2.next())
                    steamid = rs2.getString("steamid");
                rs2.close();
                if (steamid == null)
                    continue;
                boolean premium = rs.getBoolean("is_premium");
                try (Connection conn2 = DriverManager.getConnection(gmodSQL.getGModURL(), gmodSQL.getGModProperties())) {
                    Statement stmt3 = conn2.createStatement();
                    stmt3.execute("INSERT INTO donated_points (steamid,points,is_premium,server) VALUES(" + steamid + ',' + points + ',' + premium + ",\"" + server + "\")");
                    stmt3.close();
                    stmt2.execute("DELETE FROM ps2_info WHERE id = " + rs.getInt("id"));//Delete the row
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            rs.close();
            stmt.close();
            stmt2.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks donations made for ranks.
     */
    private void checkRanks() {
        ForumMySQL forumMySQL = (ForumMySQL) Janet.getForums().getMySQL();
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM donation_info");
            while (rs.next()) {
                if (stop)
                    break;
                int siteID = rs.getInt("website_id");
                int rankID = rs.getInt("rank_id");
                boolean given = rs.getBoolean("given");
                Date expires = rs.getDate("expires");
                boolean expired = expires != null && expires.getTime() <= Calendar.getInstance().getTime().getTime();
                if (expired) {
                    if (given) {
                        if (forumMySQL.removeRank(siteID, rankID)) //Remove the row from the table
                            stmt2.execute("DELETE FROM donation_info WHERE website_id = " + siteID + " AND rank_id = " + rankID);//Delete the row
                    } else
                        stmt2.execute("DELETE FROM donation_info WHERE website_id = " + siteID + " AND rank_id = " + rankID);//Delete the row
                } else if (!given) {
                    if (forumMySQL.addRank(siteID, rankID)) {
                        if (expires == null)
                            stmt2.execute("DELETE FROM donation_info WHERE website_id = " + siteID + " AND rank_id = " + rankID);//Delete the row
                        else
                            stmt2.executeUpdate("UPDATE donation_info SET given = true WHERE website_id =" + siteID + " AND rank_id = " + rankID);//Mark as given
                    }
                }
            }
            rs.close();
            stmt.close();
            stmt2.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}