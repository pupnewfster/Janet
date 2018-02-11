package gg.galaxygaming.janetissuetracker.Slack;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import gg.galaxygaming.janetissuetracker.CommandHandler.RankTree;

public class SlackUser implements BaseSlackUser {
    private final String id;
    private final String name;
    private RankTree rank;

    SlackUser(JsonObject json) {
        this.id = json.getString(Jsoner.mintJsonKey("id", null));
        this.name = json.getString(Jsoner.mintJsonKey("name", null));
        if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_bot", false)))
            this.rank = RankTree.BOT;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_primary_owner", false)) || json.getBooleanOrDefault(Jsoner.mintJsonKey("is_owner", false)))
            this.rank = RankTree.EXECSTAFF;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_admin", false)))
            this.rank = RankTree.ADMIN;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_ultra_restricted", false)) || json.getBooleanOrDefault(Jsoner.mintJsonKey("is_restricted", false)))
            this.rank = RankTree.BANNED;
        //else leave it at 0 for member
    }

    public String getName() {
        return this.name;
    }

    public String getID() {
        return this.id;
    }

    public RankTree getRank() {
        return this.rank;
    }

    public boolean isBot() {
        return getRank().equals(RankTree.BOT);
    }

    public String getRankName() {
        if (rank.equals(RankTree.EXECSTAFF))
            return "Owner";
        else if (rank.equals(RankTree.ADMIN))
            return "Admin";
        else if (rank.equals(RankTree.MEMBER))
            return "Member";
        else if (rank.isBanned())
            return "Restricted";
        return "Error";
    }
}