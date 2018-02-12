package gg.galaxygaming.janetissuetracker.CommandHandler;

public enum RankTree {//TODO probably rename this
    EXECSTAFF("Executive Staff", 7),
    DIRECTOR("Director", 6),
    MANAGER("Manager", 5),
    SRADMIN("SrAdmin", 4),
    ADMIN("Admin", 3),
    BOT("Bot", 3),
    MODERATOR("Moderator", 2),
    TMOD("TMod", 1),
    MEMBER("Member", 0),
    BANNED("Banned", -1);//TODO using the lookup table, add a column for what rank corresponds to what enum value

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