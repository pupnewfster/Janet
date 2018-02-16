package gg.galaxygaming.janet.Forums.donations;

import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.GMod.GModMySQL;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.base.AbstractMySQL;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class DonationMySQL extends AbstractMySQL {
    private final int memberID;

    public DonationMySQL() {
        super();
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        this.memberID = config.getIntegerOrDefault("FORUM_MEMBER_ID", -1);
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") || this.memberID < 0) {
            System.out.println("[ERROR] Failed to load config for connecting to MySQL Database. (Donations)");
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

    private void checkPS2() {
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM ps2_info");
            while (rs.next()) {
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
                GModMySQL gmodsql = (GModMySQL) Janet.getGMod().getMySQL();
                try (Connection conn2 = DriverManager.getConnection(gmodsql.getGModURL(), gmodsql.getGModProperties())) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkRanks() {
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM donation_info");
            while (rs.next()) {
                int siteID = rs.getInt("website_id");
                int rankID = rs.getInt("rank_id");
                boolean given = rs.getBoolean("given");
                Date expires = rs.getDate("expires");
                boolean expired = expires != null && expires.getTime() <= Calendar.getInstance().getTime().getTime();
                if (expired) {
                    if (given) {
                        if (removeRank(siteID, rankID)) //Remove the row from the table
                            stmt2.execute("DELETE FROM donation_info WHERE website_id = " + siteID + " AND rank_id = " + rankID);//Delete the row
                    } else
                        stmt2.execute("DELETE FROM donation_info WHERE website_id = " + siteID + " AND rank_id = " + rankID);//Delete the row
                } else if (!given) {
                    if (addRank(siteID, rankID)) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean addRank(int siteID, int rankID) {
        ArrayList<Integer> ranks = getRanks(siteID);
        if (ranks.contains(rankID))
            return true;
        int oldPrimary = ranks.get(0);
        ranks.add(rankID);//Add the rank to the list of ranks
        HashMap<Integer, Integer> primaries = getPrimaries(ranks);
        if (primaries.isEmpty())
            return false; //Invalid new rank
        if (ranks.contains(oldPrimary))
            for (int i = 1; i < ranks.size(); i++)
                if (primaries.get(ranks.get(i)) == oldPrimary) { //If one of secondaries still has the old primary, remove and recalculate it
                    ranks.remove(Integer.valueOf(oldPrimary));
                    break;
                }
        if (ranks.size() > 1 && ranks.contains(this.memberID))
            ranks.remove(Integer.valueOf(this.memberID)); //No longer a member
        int highest = getHighest(ranks);
        if (primaries.containsKey(highest)) {
            Integer primary = primaries.get(highest);
            if (ranks.contains(primary))//Remove it from list so that it is the list of secondaries
                ranks.remove(primary);
            return updateRanks(siteID, primary, ranks);
        } //Else something went wrong
        System.out.println("Failed to add rank " + rankID + " from " + siteID);
        return true;
    }

    private boolean removeRank(int siteID, int rankID) {
        ArrayList<Integer> ranks = getRanks(siteID);
        if (!ranks.contains(rankID))
            return true;
        HashMap<Integer, Integer> primaries = getPrimaries(ranks);
        ranks.remove(Integer.valueOf(rankID));
        if (ranks.contains(primaries.get(rankID)))
            ranks.remove(primaries.get(rankID)); //Remove old primary
        if (ranks.isEmpty())
            return updateRanks(siteID, this.memberID, ranks);
        int highest = getHighest(ranks);
        if (primaries.containsKey(highest)) {
            Integer primary = primaries.get(highest);
            if (ranks.contains(primary))//Remove it from list so that it is the list of secondaries
                ranks.remove(primary);
            return updateRanks(siteID, primary, ranks);
        } //Else something went wrong
        System.out.println("Failed to remove rank " + rankID + " from " + siteID);
        return false;
    }

    private int getHighest(ArrayList<Integer> ranks) {
        int highest = 0, highPower = 0;
        HashMap<Integer, Rank> rankPower = getRankPower(ranks);
        for (Map.Entry<Integer, Rank> entry : rankPower.entrySet()) {
            int cur = entry.getValue().getValue();
            if (cur > highPower) {
                highPower = cur;
                highest = entry.getKey();
            }
        }
        return highest;
    }

    private boolean updateRanks(int siteID, int primary, ArrayList<Integer> secondaries) {
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            StringBuilder sbGroups = new StringBuilder();
            for (int i = 0; i < secondaries.size(); i++) {
                if (i > 0)
                    sbGroups.append(',');
                sbGroups.append(secondaries.get(i));
            }
            String values = "member_group_id = " + primary + (secondaries.isEmpty() ? "" : ", mgroup_others = \"" + sbGroups.toString().trim() + '\"');
            stmt.executeUpdate("UPDATE core_members SET " + values + " WHERE member_id = " + siteID);
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private ArrayList<Integer> getRanks(int siteID) {
        ArrayList<Integer> ranks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT member_group_id, mgroup_others FROM core_members WHERE member_id = " + siteID);
            if (rs.next()) {
                int primary = rs.getInt("member_group_id");
                String secondary = rs.getString("mgroup_others");
                String[] secondaries = secondary.split(",");
                ranks.add(primary);
                for (String s : secondaries)
                    if (Utils.legalInt(s))
                        ranks.add(Integer.parseInt(s));
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ranks;
    }

    public HashMap<Integer, Integer> getPrimaries(List<Integer> ranks) {
        HashMap<Integer, Integer> rInfo = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            for (int rank : ranks) {
                ResultSet rs = stmt.executeQuery("SELECT primary_id FROM rank_priority WHERE rank_id = " + rank);
                if (rs.next()) {
                    Integer primary = rs.getInt("primary_id");
                    rInfo.put(rank, primary < 0 ? rank : primary);
                }
                rs.close();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rInfo;
    }

    public HashMap<Integer, Rank> getRankPower(List<Integer> ranks) {
        HashMap<Integer, Rank> rInfo = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            for (int rank : ranks) {
                ResultSet rs = stmt.executeQuery("SELECT rank_power FROM rank_id_lookup WHERE site_rank_id = " + rank);
                if (rs.next())
                    rInfo.put(rank, Rank.fromPower(rs.getInt("rank_power")));
                rs.close();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rInfo;
    }
}