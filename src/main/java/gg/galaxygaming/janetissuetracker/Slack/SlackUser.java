package gg.galaxygaming.janetissuetracker.Slack;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class SlackUser {
    private boolean isBot;
    private final String id;
    private final String name;
    private int rank;

    SlackUser(JsonObject json) {
        this.id = json.getString(Jsoner.mintJsonKey("id", null));
        this.name = json.getString(Jsoner.mintJsonKey("name", null));
        if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_bot", false))) {
            this.isBot = true;
            this.rank = 2;
        } else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_primary_owner", false)))
            this.rank = 3;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_owner", false)))
            this.rank = 2;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_admin", false)))
            this.rank = 1;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_ultra_restricted", false)))
            this.rank = -2;
        else if (json.getBooleanOrDefault(Jsoner.mintJsonKey("is_restricted", false)))
            this.rank = -1;
        //else leave it at 0 for member
    }

    String getName() {
        return this.name;
    }

    String getID() {
        return this.id;
    }

    int getRank() {
        return this.rank;
    }

    boolean isBot() {
        return this.isBot;
    }

    boolean isUltraRestricted() {
        return this.rank == -2;
    }

    boolean isRestricted() {
        return this.rank <= -1;
    }

    boolean isMember() {
        return this.rank >= 0;
    }

    boolean isAdmin() {
        return this.rank >= 1;
    }

    boolean isOwner() {
        return this.rank >= 2;
    }

    boolean isPrimaryOwner() {
        return this.rank == 3;
    }

    String getRankName() {
        if (isPrimaryOwner())
            return "Primary Owner";
        else if (isOwner())
            return "Owner";
        else if (isAdmin())
            return "Admin";
        else if (isMember())
            return "Member";
        else if (isUltraRestricted())
            return "Ultra Restricted";
        else if (isRestricted())
            return "Restricted";
        return "Error";
    }
}