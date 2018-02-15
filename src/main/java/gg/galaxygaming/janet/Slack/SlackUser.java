package gg.galaxygaming.janet.Slack;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import gg.galaxygaming.janet.CommandHandler.Rank;

public class SlackUser implements BaseSlackUser {
    private final String id;
    private final String name, displayName;
    private Rank rank;

    SlackUser(JsonObject json) {
        this.id = json.getString(Jsoner.mintJsonKey("id", null));
        this.name = json.getString(Jsoner.mintJsonKey("name", null));
        this.displayName = json.getStringOrDefault(Jsoner.mintJsonKey("real_name", this.name));
        if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_bot", false)))
            this.rank = Rank.BOT;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_primary_owner", false)) || json.getBooleanOrDefault(Jsoner.mintJsonKey("is_owner", false)))
            this.rank = Rank.EXECSTAFF;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_admin", false)))
            this.rank = Rank.ADMIN;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_ultra_restricted", false)) || json.getBooleanOrDefault(Jsoner.mintJsonKey("is_restricted", false)))
            this.rank = Rank.BANNED;
        else
            this.rank = Rank.MEMBER;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getID() {
        return this.id;
    }

    public Rank getRank() {
        return this.rank;
    }

    public boolean isBot() {
        return getRank().equals(Rank.BOT);
    }
}