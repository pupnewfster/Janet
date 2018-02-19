package gg.galaxygaming.janet.Forums.donations;

import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.GMod.GModMySQL;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.api.AbstractMySQL;

import javax.annotation.Nonnull;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.MySQL} to handle all MySQL
 * interactions with the tables pertaining to Donations.
 */
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks donations made for ranks.
     */
    private void checkRanks() {
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

    /**
     * Adds the specified rank to the member represented by the specified siteID.
     * @param siteID The id representing the member to add a rank to.
     * @param rankID The id representing the rank to add the member to.
     * @return True if rank was successfully added, false if something went wrong.
     */
    private boolean addRank(int siteID, int rankID) {
        List<Integer> ranks = getRanks(siteID);
        if (ranks.contains(rankID))
            return true;
        int oldPrimary = ranks.get(0);
        ranks.add(rankID);//Add the rank to the list of ranks
        Map<Integer, Integer> primaries = getPrimaries(ranks);
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
        Janet.getLogger().warn("Failed to add rank " + rankID + " from " + siteID);
        return false;
    }

    /**
     * Removes the specified rank from the member represented by the specified siteID.
     * @param siteID The id representing the member to remove a rank from.
     * @param rankID The id representing the rank to remove the member from.
     * @return True if rank was successfully removed, false if something went wrong.
     */
    private boolean removeRank(int siteID, int rankID) {
        List<Integer> ranks = getRanks(siteID);
        if (!ranks.contains(rankID))
            return true;
        Map<Integer, Integer> primaries = getPrimaries(ranks);
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
        Janet.getLogger().warn("Failed to remove rank " + rankID + " from " + siteID);
        return false;
    }

    /**
     * Calculates which rank in the input list is the highest.
     * @param ranks The list of forum rank ids to calculate the highest rank from.
     * @return The id of the highest rank in the input list.
     */
    private int getHighest(@Nonnull List<Integer> ranks) {
        int highest = 0, highPower = 0;
        Map<Integer, Rank> rankPower = getRankPower(ranks);
        for (Map.Entry<Integer, Rank> entry : rankPower.entrySet()) {
            int cur = entry.getValue().getPower();
            if (cur > highPower) {
                highPower = cur;
                highest = entry.getKey();
            }
        }
        return highest;
    }

    /**
     * Updates the list of ranks a user has on the site.
     * @param siteID      The member id to update the ranks of.
     * @param primary     The new primary rank of the member. (May be the same as it was before).
     * @param secondaries The list of secondary ranks of the member.
     * @return True if it successfully updated the ranks, false if something went wrong.
     */
    private boolean updateRanks(int siteID, int primary, @Nonnull List<Integer> secondaries) {
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

    /**
     * Gets all the ranks that the member with the specified siteID has.
     * @param siteID The id of the member to get the ranks of.
     * @return The list of all ranks the given member has.
     */
    @Nonnull
    private List<Integer> getRanks(int siteID) {
        List<Integer> ranks = new ArrayList<>();
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

    /**
     * Retrieves a map of rank ids to id of the primary rank.
     * @param ranks The list of ranks to get the primary ids of.
     * @return The map of rank ids to id of the primary rank.
     */
    @Nonnull
    private Map<Integer, Integer> getPrimaries(@Nonnull List<Integer> ranks) {
        Map<Integer, Integer> rInfo = new HashMap<>();
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

    /**
     * Retrieves a map of rank ids to the power associated with that rank.
     * @param ranks The list of ranks to get the power values of.
     * @return The map of rank ids to the power associated with that rank
     */
    @Nonnull
    private Map<Integer, Rank> getRankPower(@Nonnull List<Integer> ranks) {
        Map<Integer, Rank> rInfo = new HashMap<>();
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