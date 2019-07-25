package gg.galaxygaming.janet;

import gg.galaxygaming.janet.command_handler.CommandHandler;
import gg.galaxygaming.janet.discord.DiscordIntegration;
import gg.galaxygaming.janet.forums.ForumIntegration;
import gg.galaxygaming.janet.forums.donations.DonationIntegration;
import gg.galaxygaming.janet.gmod.GModIntegration;
import gg.galaxygaming.janet.github.GitHubIntegration;
import gg.galaxygaming.janet.slack.SlackIntegration;
import gg.galaxygaming.janet.teamspeak.TeamSpeakIntegration;
import gg.galaxygaming.janet.api.Integration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    private GitHubIntegration github;
    private SlackIntegration slack;
    private GModIntegration gmod;
    private ForumIntegration forum;

    private Janet() {//TODO Throw exceptions if something failed to initialize, and then add @Nullable to the get methods
        INSTANCE = this;
        this.logger = LogManager.getLogger("Janet");
        this.config = new Config();
        this.cmdHandler = new CommandHandler("gg.galaxygaming.janet.CommandHandler.Commands");
        if (config.getOrDefault("SLACK_ENABLED", false)) {
            this.slack = new SlackIntegration();
        }
        this.forum = new ForumIntegration();
        //this.github = new GitHubIntegration();
        this.discord = new DiscordIntegration();
        this.teamspeak = new TeamSpeakIntegration();
        this.gmod = new GModIntegration();
        this.donations = new DonationIntegration();//This has to be after gmod initialization
    }

    /**
     * Called when {@link Janet} is stopped to gracefully close all open {@link gg.galaxygaming.janet.api.Integration}s.
     */
    public void stop() {
        stopIntegration(getForums());
        stopIntegration(getGitHub());
        stopIntegration(getSlack());
        stopIntegration(getGMod());
        stopIntegration(getDiscord());
        stopIntegration(getTeamspeak());
        stopIntegration(getDonations());
    }

    private void stopIntegration(Integration integration) {
        if (integration != null) {
            integration.stop();
        }
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
    @Nullable
    public static SlackIntegration getSlack() {
        return INSTANCE.slack;
    }

    /**
     * Retrieves the {@link GitHubIntegration}.
     * @return The {@link GitHubIntegration}.
     */
    @Nullable
    public static GitHubIntegration getGitHub() {
        return INSTANCE.github;
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