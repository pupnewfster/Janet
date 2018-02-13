package gg.galaxygaming.janet;

import gg.galaxygaming.janet.CommandHandler.CommandHandler;
import gg.galaxygaming.janet.Discord.DiscordIntegration;
import gg.galaxygaming.janet.Forums.RestIntegration;
import gg.galaxygaming.janet.GMod.GModIntegration;
import gg.galaxygaming.janet.TeamSpeak.TeamSpeakIntegration;

public class Janet {//TODO: add in proper javadoc explanations for methods
    //TODO make interfaces for integrations and mysql classes??
    private static Janet INSTANCE;
    public static boolean DEBUG = true;//TODO replace with a proper logger
    private final Config config;
    private final CommandHandler cmdHandler;
    private DiscordIntegration discord;
    private TeamSpeakIntegration teamspeak;
    private GModIntegration gmod;
    //private SlackIntegration slack;
    private RestIntegration rest;

    private Janet() {
        INSTANCE = this;
        this.config = new Config();
        //TODO: Potentially improve threading, rather than having it all in main thread
        this.cmdHandler = new CommandHandler("gg.galaxygaming.janet.CommandHandler.Commands");
        //this.slack = new SlackIntegration();//Disabled
        this.discord = new DiscordIntegration();
        this.teamspeak = new TeamSpeakIntegration();
        this.gmod = new GModIntegration();
        //this.rest = new RestIntegration(this.config);//TODO bring back when we work on automating suggestions -> githuhb
    }

    //TODO call this method
    public void stop() {
        //getRestIntegration().stop();
        //getSlack().disconnect();
        if (getGMod() != null)
            getGMod().stop();
        if (getDiscord() != null)
            getDiscord().stop();
        if (getTeamspeak() != null)
            getTeamspeak().stop();
    }


    public static void main(String[] args) {
        new Janet();
        Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::stop));//TODO test
    }

    public static Config getConfig() {
        return INSTANCE.config;
    }

    /*public static SlackIntegration getSlack() {
        return INSTANCE.slack;
    }*/

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

    public static CommandHandler getCommandHandler() {
        return INSTANCE.cmdHandler;
    }

    public static Janet getInstance() {
        return INSTANCE;
    }
}