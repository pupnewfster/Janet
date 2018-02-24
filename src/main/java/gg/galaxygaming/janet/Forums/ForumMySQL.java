package gg.galaxygaming.janet.Forums;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.api.AbstractMySQL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.MySQL} to handle all MySQL interactions
 * with the Forum tables and ranks.
 */
public class ForumMySQL extends AbstractMySQL {//TODO: Should some of application and suggestion stuff be cached in a mysql table
    private final int memberID;

    public ForumMySQL() {
        super();
        Config config = Janet.getConfig();
        String dbName = config.getStringOrDefault("DB_NAME", "database");
        String dbUser = config.getStringOrDefault("DB_USER", "user");
        String dbPass = config.getStringOrDefault("DB_PASSWORD", "password");
        this.memberID = config.getIntegerOrDefault("FORUM_MEMBER_ID", -1);
        if (dbName.equals("database") || dbPass.equals("password") || dbUser.equals("user") || this.memberID < 0) {
            Janet.getLogger().error("Failed to load config for connecting to MySQL Database. (Forums)");
            return;
        }
        this.url = "jdbc:mysql://" + config.getStringOrDefault("DB_HOST", "127.0.0.1:3306") + '/' + dbName;
        properties.setProperty("user", dbUser);
        properties.setProperty("password", dbPass);
        this.service = "Forums";
        //this.checkThread.start();
    }

    /**
     * Does not do anything as of the moment.
     */
    @Override
    protected void checkAll() {
    }

    /**
     * Gets all the ranks that the member with the specified siteID has.
     * @param siteID The id of the member to get the ranks of.
     * @return The list of all ranks the given member has.
     */
    @Nonnull
    public List<Integer> getRanks(int siteID) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ranks;
    }

    /**
     * Gets the query for selecting all ranks the member with the given siteID has.
     * @param siteID The siteID of the member to check.
     * @return The query for selecting all ranks the member with the given siteID has, or null if no member was found with the given siteID.
     */
    @Nullable
    public String getRankQuery(int siteID) {
        List<Integer> ranks = getRanks(siteID);
        if (ranks.isEmpty())
            return null;
        StringBuilder sbGroups = new StringBuilder();
        for (int s : ranks)
            sbGroups.append(',').append(s);
        String q = sbGroups.toString().trim().substring(1);
        return ranks.size() == 1 ? "site_rank_id = " + q : "site_rank_id IN (" + q + ')';
    }

    /**
     * Adds the specified rank to the member represented by the specified siteID.
     * @param siteID The id representing the member to add a rank to.
     * @param rankID The id representing the rank to add the member to.
     * @return True if rank was successfully added, false if something went wrong.
     */
    public boolean addRank(int siteID, int rankID) {
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
        Janet.getLogger().warn("Failed to add rank " + rankID + " to " + siteID);
        return false;
    }

    /**
     * Removes the specified rank from the member represented by the specified siteID.
     * @param siteID The id representing the member to remove a rank from.
     * @param rankID The id representing the rank to remove the member from.
     * @return True if rank was successfully removed, false if something went wrong.
     */
    public boolean removeRank(int siteID, int rankID) {
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
     * Replaces one rank with another for the member with the given id.
     * @param siteID    The id representing the member to change the ranks of.
     * @param oldRankID The id representing the rank to remove the member from.
     * @param newRankID The id representing the rank to add the member to.
     * @return True if the ranks were successfully switched, false if something went wrong.
     */
    public boolean replaceRank(int siteID, int oldRankID, int newRankID) {
        if (oldRankID == newRankID)
            return true;
        List<Integer> ranks = getRanks(siteID);
        if (ranks.contains(newRankID))
            return true;
        int oldPrimary = ranks.get(0);
        ranks.add(newRankID);//Add the rank to the list of ranks
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

        if (oldRankID > 0) {
            ranks.remove(Integer.valueOf(oldRankID));
            if (ranks.contains(primaries.get(oldRankID)))
                ranks.remove(primaries.get(oldRankID)); //Remove old primary
            if (ranks.isEmpty())
                return updateRanks(siteID, this.memberID, ranks);
        }
        int highest = getHighest(ranks);
        if (primaries.containsKey(highest)) {
            Integer primary = primaries.get(highest);
            if (ranks.contains(primary))//Remove it from list so that it is the list of secondaries
                ranks.remove(primary);
            return updateRanks(siteID, primary, ranks);
        } //Else something went wrong
        Janet.getLogger().warn("Failed to change rank " + oldRankID + " to " + newRankID);
        return false;
    }

    /**
     * Calculates which rank in the input list is the highest.
     * @param ranks The list of forum rank ids to calculate the highest rank from.
     * @return The id of the highest rank in the input list.
     */
    public int getHighest(@Nonnull List<Integer> ranks) {
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
    public boolean updateRanks(int siteID, int primary, @Nonnull List<Integer> secondaries) {
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
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Retrieves a map of rank ids to id of the primary rank.
     * @param ranks The list of ranks to get the primary ids of.
     * @return The map of rank ids to id of the primary rank.
     */
    @Nonnull
    public Map<Integer, Integer> getPrimaries(@Nonnull List<Integer> ranks) {
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
        } catch (SQLException e) {
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
    public Map<Integer, Rank> getRankPower(@Nonnull List<Integer> ranks) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rInfo;
    }

    /**
     * Retrieves a {@link Map} of all open applications for the given server.
     * @param server The server to get all open applications of.
     * @return A {@link Map} of topic id to topic name.
     */
    @Nullable
    public Map<Integer, String> getApplicationNames(String server) {
        Map<Integer, String> applications = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT forum_id FROM forum_info WHERE forum_name = \"" + server + '"');
            if (rs.next()) {
                int forumID = rs.getInt("forum_id");
                JsonObject forum = Janet.getForums().sendGET("/forums/topics?forums=" + forumID);
                if (forum == null) {
                    rs.close();
                    stmt.close();
                    return null;
                }
                JsonArray rTopics = (JsonArray) forum.get("results");
                for (Object t : rTopics) {
                    JsonObject topic = (JsonObject) t;
                    Integer topicID = topic.getIntegerOrDefault(Jsoner.mintJsonKey("id", null));
                    String title = topic.getStringOrDefault(Jsoner.mintJsonKey("title", null));
                    boolean hidden = topic.getBooleanOrDefault(Jsoner.mintJsonKey("hidden", false));
                    boolean archived = topic.getBooleanOrDefault(Jsoner.mintJsonKey("archived", false));
                    boolean pinned = topic.getBooleanOrDefault(Jsoner.mintJsonKey("pinned", false));
                    if (topicID != null && title != null && !hidden && !archived && !pinned) {
                        JsonObject post = (JsonObject) topic.get("firstPost");
                        JsonObject author = (JsonObject) post.get("author");
                        applications.put(topicID, title + ", Created by: " + author.getStringOrDefault(Jsoner.mintJsonKey("name", "unknown")) + '.');
                    }
                }
            } else {
                rs.close();
                stmt.close();
                return null;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return applications;
    }

    /**
     * Accepts the application with the given id, and then returns what went wrong or that the acceptance was successful.
     * @param id The id of the application to accept.
     * @return The message to display, whether it was accepted or what went wrong.
     */
    @Nonnull
    public String acceptApp(int id) {
        ForumIntegration forums = Janet.getForums();
        JsonObject topic = forums.sendGET("/forums/topics/" + id);
        if (topic == null)
            return "ERROR: Failed to get topic information from id: " + id + ". Please check that the id is valid.";
        if (topic.containsKey("errorCode"))
            return "An error occurred finding the topic with the id :" + id + ". Error Code:" + topic.getStringOrDefault(Jsoner.mintJsonKey("errorMessage", "UNKNOWN"));
        JsonObject f = (JsonObject) topic.get("forum");
        int forumId = f.getIntegerOrDefault(Jsoner.mintJsonKey("id", -1));
        JsonObject post = (JsonObject) topic.get("firstPost");
        JsonObject author = (JsonObject) post.get("author");
        int siteID = author.getIntegerOrDefault(Jsoner.mintJsonKey("id", -1));
        if (siteID < 0)
            return "ERROR: Something went wrong retrieving the identity of the topic starter.";
        //While getting topic id get the member id
        if (forumId < 0)
            return "ERROR: Unable to find application with the given id.";
        String server = null;
        int acceptedForum = -1;
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT accepted_id, forum_name FROM forum_info WHERE forum_id = " + forumId);
            if (rs.next()) {
                acceptedForum = rs.getInt("accepted_id");
                server = rs.getString("forum_name");
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (acceptedForum < 0 || server == null)
            return "ERROR: Unable to find accepted forum. This is probably because the given post is not in an applications forum.";
        Map<Integer, Integer> rankMap = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(this.url, this.properties)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT old_rank, new_rank FROM forum_promote_info WHERE server = \"" + server + '"');
            while (rs.next())
                rankMap.put(rs.getInt("old_rank"), rs.getInt("new_rank"));
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (rankMap.isEmpty())
            return "ERROR: No rank info found.";
        int oldRank = -1;
        List<Integer> ranks = getRanks(siteID);
        boolean rankFound = false;
        for (int rank : ranks)
            if (rankMap.containsKey(rank)) {
                if (rankFound) //If someone screwed up and they have both TMod and Mod for example.
                    return "ERROR: This user has more than one staff rank for this server.";
                oldRank = rank;
                rankFound = true;
            }
        int newRank;
        if (rankMap.containsKey(oldRank))
            newRank = rankMap.get(oldRank);
        else
            return "ERROR: No available rank found to promote member to. This is likely because they are already the highest rank autopromote can set them to.";
        JsonObject lockMove = new JsonObject();
        lockMove.put("locked", 1);
        lockMove.put("forum", acceptedForum);
        JsonObject moveResponse = forums.sendPOST("/forums/topics/" + id, lockMove);
        if (moveResponse == null)
            return "ERROR: Unable to move topic: " + id + '.';
        if (moveResponse.containsKey("errorCode"))
            return "An error occurred moving and locking the topic. Error Code:" + moveResponse.getStringOrDefault(Jsoner.mintJsonKey("errorMessage", "UNKNOWN"));
        JsonObject accept = new JsonObject();
        accept.put("topic", id);
        accept.put("author", forums.getJanetID());
        accept.put("post", "<p>" + forums.getAcceptMessage() + "</p>");
        JsonObject forum = forums.sendPOST("/forums/posts", accept);
        if (forum == null)
            return "ERROR: Unable to find forum: " + forumId + '.';
        if (forum.containsKey("errorCode"))
            return "An error occurred posting the acceptance message. Error Code:" + forum.getStringOrDefault(Jsoner.mintJsonKey("errorMessage", "UNKNOWN"));
        if (!replaceRank(siteID, oldRank, newRank))
            return "Failed to change rank " + oldRank + " to " + newRank;
        return "Successfully accepted the application with the id: " + id + '.';
    }
}