package gg.galaxygaming.janetissuetracker.CommandHandler;

public enum RankTree {//TODO probably rename this
    OWNER("Owner", 5),
    DIRECTOR("Director", 4),
    MANAGER("Manager", 3),
    ADMIN("Admin", 2),
    BOT("Bot", 2),
    MODERATOR("Moderator", 1),
    MEMBER("Member", 0),
    BANNED("Banned", -1);

    private String name;
    private int value;

    RankTree(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

    public boolean hasRank(RankTree rank) {
        return this.value >= rank.value;
    }

    public boolean isBanned() {
    return this.equals(BANNED);
    }
}