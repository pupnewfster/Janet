package gg.galaxygaming.janet.Slack;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.api.AbstractSlackUser;

/**
 * An implementation for users of {@link AbstractSlackUser}
 */
public class SlackUser implements AbstractSlackUser {
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

    /**
     * Retrieves the name of this {@link SlackUser}.
     * @return The name of this {@link SlackUser}.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Retrieves the display name of this {@link SlackUser}.
     * @return The display name of this {@link SlackUser}.
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Retrieves the Slack ID of this {@link SlackUser}.
     * @return The Slack ID of this {@link SlackUser}.
     */
    public String getID() {
        return this.id;
    }

    /**
     * Retrieves the {@link Rank} of this {@link SlackUser}.
     * @return The {@link Rank} of this {@link SlackUser}.
     */
    public Rank getRank() {
        return this.rank;
    }

    /**
     * Checks if this {@link SlackUser} is a bot. In the future this will be replaced with {@link gg.galaxygaming.janet.Slack.BotUser}.
     * @return True if this user is a bot, false otherwise.
     */
    public boolean isBot() {
        return getRank().equals(Rank.BOT);
    }
}