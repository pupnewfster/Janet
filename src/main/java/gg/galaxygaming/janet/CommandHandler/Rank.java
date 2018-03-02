package gg.galaxygaming.janet.CommandHandler;

import javax.annotation.Nonnull;
import java.awt.Color;

/**
 * An enum representing the {@link Rank} structure of our community.
 */
public enum Rank {
    /**
     * {@link Rank} representing the Executive Staff.
     * <p>
     * This includes the Owner, Executive Director, and Head Developer.
     */
    EXECUTIVE_STAFF("Executive Staff", 14, new Color(0xFF0000)),
    /**
     * {@link Rank} representing all the Directors of our servers.
     */
    DIRECTOR("Director", 13, new Color(0xFF0000)),
    /**
     * {@link Rank} representing all the Managers of our servers.
     */
    MANAGER("Manager", 12, new Color(0xFFCC00)),
    /**
     * {@link Rank} representing all the Senior Admins of our servers.
     */
    SENIOR_ADMIN("Senior Admin", 11, new Color(0xFF6600)),
    /**
     * {@link Rank} representing all Developers.
     */
    DEV("Developer", 10, new Color(0xA00CA3)),
    /**
     * {@link Rank} representing all the Admins of our servers.
     */
    ADMIN("Admin", 9, new Color(0xFF9900)),
    /**
     * {@link Rank} representing all the Moderators of our servers.
     */
    MODERATOR("Moderator", 8, new Color(0x00CCFF)),
    /**
     * {@link Rank} representing any bots.
     */
    BOT("Bot", 7, new Color(0xFF0000)),
    /**
     * {@link Rank} representing all the Trial Moderators of our servers.
     */
    TMOD("Trial Mod", 6, new Color(0xD900FF)),
    /**
     * {@link Rank} representing all Trial Developers.
     */
    TDEV("Trial Dev", 5, new Color(0xD900FF)),
    /**
     * {@link Rank} representing all the Donor Moderators of our servers.
     */
    DMOD("Donor Mod", 4, new Color(0x98EFF5)),
    /**
     * {@link Rank} representing all the people with Emeritus.
     */
    EMERITUS("Emeritus", 3, new Color(0xEB1FF2)),
    /**
     * {@link Rank} representing all the people with Community Supporter.
     */
    COMMUNITY_SUPPORTER("Community Supporter", 2, new Color(0x1F95EF)),
    /**
     * {@link Rank} representing all the people who are verified as members of our community.
     */
    MEMBER("Member", 1, new Color(0x00E100)),
    /**
     * {@link Rank} representing all Guests.
     */
    GUEST("Guest", 0, new Color(0xFFFFFF)),
    /**
     * {@link Rank} representing anyone who is banned. Currently an unused rank.
     */
    BANNED("Banned", -1, new Color(0x515151));

    private final String name;
    private final int power;
    private final Color color;

    Rank(String name, int power, Color color) {
        this.name = name;
        this.power = power;
        this.color = color;
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
    @Nonnull
    public String getName() {
        return this.name;
    }

    /**
     * Retrieves the {@link Color} that this {@link Rank} displays as.
     * @return The {@link Color} that this {@link Rank} displays as.
     */
    @Nonnull
    public Color getColor() {
        return this.color;
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