package gg.galaxygaming.janet.Slack;

//TODO add a usefull param?
public enum InviteResponse {
    ALREADY_INVITED("already_invited", false),
    ALREADY_IN_TEAM("already_in_team", false),
    CHANNEL_NOT_FOUND("channel_not_found", false),
    SENT_RECENTLY("sent_recently", false),
    USER_DISABLED("user_disabled", true),//Send message about re-enabled and also reactivate them
    MISSING_SCOPE("missing_scope", false),
    INVALID_EMAIL("invalid_email", true),
    NOT_ALLOWED("not_allowed", false),
    SUCCESS("success", true),//Does not exist
    OTHER("other", false);//Does not exist

    private final String message;
    private final boolean isUseful;

    InviteResponse(String message, boolean isUseful) {
        this.message = message;
        this.isUseful = isUseful;
    }

    public static InviteResponse fromString(String response) {//TODO potentially improve this method so that it maps them instead of just relying on valueOf
        return valueOf(response.toUpperCase());
    }

    public boolean isUseful() {
        return this.isUseful;
    }

    public String getMessage() {
        return this.message;
    }
}