package gg.galaxygaming.janetissuetracker;

import gg.galaxygaming.janetissuetracker.CommandHandler.CommandHandler;
import gg.galaxygaming.janetissuetracker.Discord.DiscordIntegration;
import gg.galaxygaming.janetissuetracker.Forums.RestIntegration;
import gg.galaxygaming.janetissuetracker.TeamSpeak.TeamSpeakIntegration;

public class IssueTracker {//TODO: add in proper javadoc explanations for methods
    private static IssueTracker INSTANCE;
    public static boolean DEBUG = true;//TODO replace with a proper logger
    private Config config;
    private DiscordIntegration discord;
    private TeamSpeakIntegration teamspeak;
    //private JanetSlack slack;
    private RestIntegration rest;
    private final CommandHandler cmdHandler;

    private IssueTracker() {
        this.config = new Config();
        //TODO: Potentially improve threading, rather than having it all in main thread
        //TODO: Create exceptions for the two below ones to throw if the required initialization configs are not met
        this.cmdHandler = new CommandHandler("gg.galaxygaming.janetissuetracker.CommandHandler.Commands");
        //this.slack = new JanetSlack(this.config);//Disabled
        this.discord = new DiscordIntegration(this.config);
        //this.rest = new RestIntegration(this.config);//TODO bring back when we work on automating suggestions -> githuhb
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));//TODO test
    }

    //TODO call this method
    public void stop() {
        //getRestIntegration().stop();
        //getSlack().disconnect();
        getDiscord().stop();
    }


    public static void main(String[] args) {
        INSTANCE = new IssueTracker();
    }

    public static Config getConfig() {
        return INSTANCE.config;
    }

    /*public static JanetSlack getSlack() {
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

    public static CommandHandler getCommandHandler() {
        return INSTANCE.cmdHandler;
    }

    public static IssueTracker getInstance() {
        return INSTANCE;
    }
}