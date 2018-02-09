package gg.galaxygaming.janetissuetracker.CommandHandler;

import gg.galaxygaming.janetissuetracker.IssueTracker;
import gg.galaxygaming.janetissuetracker.Slack.SlackUser;

public class CommandSender {
    private CommandSource source;
    private SlackUser slackUser;
    private String channel;
    private RankTree rank;

    public CommandSender(SlackUser user, String channel) {
        this.source = CommandSource.Slack;
        this.slackUser = user;
        this.channel = channel;
        this.rank = this.slackUser.getRank();
    }

    public CommandSource getSource() {
        return this.source;
    }

    public SlackUser getSlackUser() {
        return this.slackUser;
    }

    public RankTree getRank() {
        return this.rank;
    }

    public void sendMessage(String message) {
        switch (this.source) {
            case Slack:
                IssueTracker.getSlack().sendMessage(message, this.channel);
                break;
            case Console:
                System.out.println(message);
                break;
            //TODO implement below methods
            case TeamSpeak:
                break;
            case Discord:
                break;
            default:
                break;
        }
    }
}