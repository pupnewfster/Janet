package gg.galaxygaming.janet;

import gg.galaxygaming.janet.CommandHandler.CommandHandler;
import gg.galaxygaming.janet.Discord.DiscordIntegration;
import gg.galaxygaming.janet.Forums.ForumIntegration;
import gg.galaxygaming.janet.Forums.donations.DonationIntegration;
import gg.galaxygaming.janet.GMod.GModIntegration;
import gg.galaxygaming.janet.Slack.SlackIntegration;
import gg.galaxygaming.janet.TeamSpeak.TeamSpeakIntegration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

/**
 * The main class.
 */
public class Janet {
    private static Janet INSTANCE;
    private final Logger logger;//TODO add printStackTract to logger?
    private final Config config;
    private final CommandHandler cmdHandler;
    private TeamSpeakIntegration teamspeak;
    private DonationIntegration donations;
    private DiscordIntegration discord;
    private SlackIntegration slack;
    private GModIntegration gmod;
    private ForumIntegration forum;

    private Janet() {//TODO Throw exceptions if something failed to initialize, and then add @Nullable to the get methods
        INSTANCE = this;
        this.logger = LogManager.getLogger("Janet");
        this.config = new Config();
        this.cmdHandler = new CommandHandler("gg.galaxygaming.janet.CommandHandler.Commands");
        this.slack = new SlackIntegration();
        this.forum = new ForumIntegration();
        this.discord = new DiscordIntegration();
        this.teamspeak = new TeamSpeakIntegration();
        this.gmod = new GModIntegration();
        this.donations = new DonationIntegration();//This has to be after gmod initialization
    }

    /**
     * Called when {@link Janet} is stopped to gracefully close all open {@link gg.galaxygaming.janet.api.Integration}s.
     */
    public void stop() {
        if (getForums() != null)
            getForums().stop();
        if (getSlack() != null)
            getSlack().stop();
        if (getGMod() != null)
            getGMod().stop();
        if (getDiscord() != null)
            getDiscord().stop();
        if (getTeamspeak() != null)
            getTeamspeak().stop();
    }

    public static void main(String[] args) {
        new Janet();
        Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::stop));
    }

    /**
     * Retrieves the {@link Config}.
     * @return The {@link Config}.
     */
    @Nonnull
    public static Config getConfig() {
        return INSTANCE.config;
    }

    /**
     * Retrieves the {@link SlackIntegration}.
     * @return The {@link SlackIntegration}.
     */
    public static SlackIntegration getSlack() {
        return INSTANCE.slack;
    }

    /**
     * Retrieves the {@link ForumIntegration}.
     * @return The {@link ForumIntegration}.
     */
    public static ForumIntegration getForums() {
        return INSTANCE.forum;
    }

    /**
     * Retrieves the {@link DiscordIntegration}.
     * @return The {@link DiscordIntegration}.
     */
    public static DiscordIntegration getDiscord() {
        return INSTANCE.discord;
    }

    /**
     * Retrieves the {@link TeamSpeakIntegration}.
     * @return The {@link TeamSpeakIntegration}.
     */
    public static TeamSpeakIntegration getTeamspeak() {
        return INSTANCE.teamspeak;
    }

    /**
     * Retrieves the {@link GModIntegration}.
     * @return The {@link GModIntegration}.
     */
    public static GModIntegration getGMod() {
        return INSTANCE.gmod;
    }

    /**
     * Retrieves the {@link DonationIntegration}.
     * @return The {@link DonationIntegration}.
     */
    public static DonationIntegration getDonations() {
        return INSTANCE.donations;
    }

    /**
     * Retrieves the {@link CommandHandler}.
     * @return The {@link CommandHandler}.
     */
    @Nonnull
    public static CommandHandler getCommandHandler() {
        return INSTANCE.cmdHandler;
    }

    /**
     * Retrieves the {@link Logger}.
     * @return The {@link Logger}.
     */
    @Nonnull
    public static Logger getLogger() {
        return INSTANCE.logger;
    }

    /**
     * Retrieves the nonstatic instance of {@link Janet}.
     * @return The nonstatic instance of {@link Janet}.
     */
    @Nonnull
    public static Janet getInstance() {
        return INSTANCE;
    }
}