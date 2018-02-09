package gg.galaxygaming.janetissuetracker;

import gg.galaxygaming.janetissuetracker.Forums.RestIntegration;
import gg.galaxygaming.janetissuetracker.Slack.JanetSlack;

public class IssueTracker {
    private static IssueTracker INSTANCE;
    public static boolean DEBUG = true;//TODO replace with a proper loger
    private final Config config;
    private final JanetSlack slack;
    private final RestIntegration rest;

    private IssueTracker() {
        this.config = new Config();
        //TODO: Potentially improve threading, rather than having it all in main thread
        //TODO: Create exceptions for the two below ones to throw if the required initialization configs are not met
        this.slack = new JanetSlack(this.config);
        this.rest = new RestIntegration(this.config);
    }

    //TODO create a stop method to call things like JanetSlack disconnect


    public static void main(String[] args) {
        INSTANCE = new IssueTracker();
    }

    public static Config getConfig() {
        return INSTANCE.config;
    }

    public static JanetSlack getSlack() {
        return INSTANCE.slack;
    }

    public static RestIntegration getRestIntegration() {
        return INSTANCE.rest;
    }

    public static IssueTracker getInstance() {
        return INSTANCE;
    }
}