package gg.galaxygaming.janet;

import gg.galaxygaming.janet.CommandHandler.CommandHandler;
import gg.galaxygaming.janet.Discord.DiscordIntegration;
import gg.galaxygaming.janet.Forums.RestIntegration;
import gg.galaxygaming.janet.Forums.donations.DonationIntegration;
import gg.galaxygaming.janet.GMod.GModIntegration;
import gg.galaxygaming.janet.Slack.SlackIntegration;
import gg.galaxygaming.janet.TeamSpeak.TeamSpeakIntegration;

public class Janet {//TODO: add in proper javadoc explanations for methods
    private static Janet INSTANCE;
    public static final boolean DEBUG = true;//TODO replace with a proper logger
    private final Config config;
    private final CommandHandler cmdHandler;
    private TeamSpeakIntegration teamspeak;
    private DonationIntegration donations;
    private DiscordIntegration discord;
    private SlackIntegration slack;
    private GModIntegration gmod;
    private RestIntegration rest;

    private Janet() {
        INSTANCE = this;
        this.config = new Config();
        this.cmdHandler = new CommandHandler("gg.galaxygaming.janet.CommandHandler.Commands");
        this.slack = new SlackIntegration();
        //this.rest = new RestIntegration(this.config);//TODO bring back when we work on automating suggestions -> github
        this.discord = new DiscordIntegration();
        this.teamspeak = new TeamSpeakIntegration();
        this.gmod = new GModIntegration();
        this.donations = new DonationIntegration();//THIS has to be after gmod initialization
    }

    public void stop() {
        if (getRestIntegration() != null)
            getRestIntegration().stop();
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

    public static Config getConfig() {
        return INSTANCE.config;
    }

    public static SlackIntegration getSlack() {
        return INSTANCE.slack;
    }

    public static RestIntegration getRestIntegration() {
        return INSTANCE.rest;
    }

    public static DiscordIntegration getDiscord() {
        return INSTANCE.discord;
    }

    public static TeamSpeakIntegration getTeamspeak() {
        return INSTANCE.teamspeak;
    }

    public static GModIntegration getGMod() {
        return INSTANCE.gmod;
    }

    public static DonationIntegration getDonations() {
        return INSTANCE.donations;
    }

    public static CommandHandler getCommandHandler() {
        return INSTANCE.cmdHandler;
    }

    public static Janet getInstance() {
        return INSTANCE;
    }
}