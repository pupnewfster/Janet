package gg.galaxygaming.janet.CommandHandler;

import javax.annotation.Nonnull;

/**
 * An enum representing the {@link Rank} structure of our community.
 */
public enum Rank {//TODO: Store the color the rank should have in this so that when TeamSpeak Widget is made it can also have colored things
    /**
     * {@link Rank} representing the Executive Staff.
     * <p>
     * This includes the Owner, Executive Director, and Head Developer.
     */
    EXECUTIVE_STAFF("Executive Staff", 12),
    /**
     * {@link Rank} representing all the Directors of our servers.
     */
    DIRECTOR("Director", 11),
    /**
     * {@link Rank} representing all the Managers of our servers.
     */
    MANAGER("Manager", 10),
    /**
     * {@link Rank} representing all the Senior Admins of our servers.
     */
    SENIOR_ADMIN("Senior Admin", 9),
    /**
     * {@link Rank} representing all Developers.
     */
    DEV("Developer", 8),
    /**
     * {@link Rank} representing all the Admins of our servers.
     */
    ADMIN("Admin", 7),
    /**
     * {@link Rank} representing all the Moderators of our servers.
     */
    MODERATOR("Moderator", 6),
    /**
     * {@link Rank} representing any bots.
     */
    BOT("Bot", 5),
    /**
     * {@link Rank} representing all the Trial Moderators of our servers.
     */
    TMOD("Trial Mod", 4),
    /**
     * {@link Rank} representing all Trial Developers.
     */
    TDEV("Trial Dev", 3),
    /**
     * {@link Rank} representing all the Donor Moderators of our servers.
     */
    DMOD("Donor Mod", 2),
    /**
     * {@link Rank} representing all the people who are verified as members of our community.
     */
    MEMBER("Member", 1),
    /**
     * {@link Rank} representing all Guests.
     */
    GUEST("Guest", 0),
    /**
     * {@link Rank} representing anyone who is banned. Currently an unused rank.
     */
    BANNED("Banned", -1);

    private final String name;
    private final int power;

    Rank(String name, int power) {
        this.name = name;
        this.power = power;
    }

    /**
     * Retrieves the {@link Rank} from the specified power level.
     * @param power An integer representing how much power the {@link Rank} being searched for has.
     * @return The {@link Rank} from the specified power level.
     */
    public static Rank fromPower(int power) {
        for (Rank t : values())
            if (t.power == power)
                return t;
        return GUEST;
    }

    /**
     * Retrieves the integer representation of how much power thia {@link Rank} has.
     * @return The power this {@link Rank} has.
     */
    public int getPower() {
        return this.power;
    }

    /**
     * Retrieves the proper name representing this {@link Rank}.
     * @return The proper name of this {@link Rank}.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Checks if this {@link Rank} is the same or above the specified {@link Rank}.
     * @param rank The {@link Rank} to check.
     * @return True if this {@link Rank}'s power is the same or higher than the given {@link Rank}'s power.
     */
    public boolean hasRank(@Nonnull Rank rank) {
        return this.power >= rank.power;
    }

    /**
     * Retrieves if this {@link Rank} is equal to BANNED. This is a separate method from {@link #hasRank(Rank)},
     * because all ranks have more power than {@link #BANNED}.
     * @return True if this {@link Rank} is the same as {@link #BANNED}.
     */
    public boolean isBanned() {
        return this.equals(BANNED);
    }
}