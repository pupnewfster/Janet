package gg.galaxygaming.janet.Slack;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.api.AbstractSlackUser;

import javax.annotation.Nonnull;

/**
 * An implementation for users of {@link AbstractSlackUser}
 */
public final class SlackUser implements AbstractSlackUser {
    private final String id;
    private final String name, displayName;
    private final Rank rank;

    SlackUser(JsonObject json) {
        this.id = json.getString(Jsoner.mintJsonKey("id", null));
        this.name = json.getString(Jsoner.mintJsonKey("name", null));
        this.displayName = json.getStringOrDefault(Jsoner.mintJsonKey("real_name", this.name));
        if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_bot", false)))
            this.rank = Rank.BOT;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_primary_owner", false)) || json.getBooleanOrDefault(Jsoner.mintJsonKey("is_owner", false)))
            this.rank = Rank.EXECUTIVE_STAFF;
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
    @Nonnull
    public String getName() {
        return this.name;
    }

    /**
     * Retrieves the display name of this {@link SlackUser}.
     * @return The display name of this {@link SlackUser}.
     */
    @Nonnull
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Retrieves the Slack ID of this {@link SlackUser}.
     * @return The Slack ID of this {@link SlackUser}.
     */
    @Nonnull
    public String getID() {
        return this.id;
    }

    /**
     * Retrieves the {@link Rank} of this {@link SlackUser}.
     * @return The {@link Rank} of this {@link SlackUser}.
     */
    @Nonnull
    public Rank getRank() {
        return this.rank;
    }

    /**
     * Checks if this {@link SlackUser} is a {@link Rank#BOT}. In the future this will be replaced with {@link gg.galaxygaming.janet.Slack.BotUser}.
     * @return True if this user is a bot, false otherwise.
     */
    public boolean isBot() {
        return this.rank.equals(Rank.BOT);
    }
}