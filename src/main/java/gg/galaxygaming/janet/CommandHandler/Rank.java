package gg.galaxygaming.janet.CommandHandler;

public enum Rank {
    EXECSTAFF("Executive Staff", 8),
    DIRECTOR("Director", 7),
    MANAGER("Manager", 6),
    SRADMIN("SrAdmin", 5),
    DEV("Dev", 4),
    ADMIN("Admin", 4),
    BOT("Bot", 4),
    MODERATOR("Moderator", 3),
    TDEV("TDev", 2),
    TMOD("TMod", 2),
    DMOD("DMod", 2),
    VIP("VIP", 1),
    MEMBER("Member", 0),
    BANNED("Banned", -1);//TODO maybe add a guest rank

    private String name;
    private int value;

    Rank(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static Rank fromPower(int power) {
        Rank[] values = values();
        for (Rank t : values) {
            if (t.getValue() == power)
                return t;
        }
        return MEMBER;
    }

    public int getValue() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

    public boolean hasRank(Rank rank) {
        return this.value >= rank.value;
    }

    public boolean isBanned() {
        return this.equals(BANNED);
    }
}