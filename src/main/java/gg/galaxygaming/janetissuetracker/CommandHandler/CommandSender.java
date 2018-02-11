package gg.galaxygaming.janetissuetracker.CommandHandler;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import gg.galaxygaming.janetissuetracker.IssueTracker;
import gg.galaxygaming.janetissuetracker.Slack.SlackUser;

public class CommandSender {
    private CommandSource source;
    private SlackUser slackUser;
    private User discordUser;
    private String channel;
    private RankTree rank;
    private boolean isPrivate;

    public CommandSender(SlackUser user, String channel) {
        this.source = CommandSource.Slack;
        this.slackUser = user;
        this.channel = channel;
        this.rank = this.slackUser.getRank();
    }

    public CommandSender(User user, String channel) {
        this(user, channel, false);
    }

    public CommandSender(User user, String channel, boolean isPrivate) {
        this.source = CommandSource.Discord;
        this.discordUser = user;
        this.channel = channel;
        this.isPrivate = isPrivate;
        this.rank = RankTree.MEMBER;//TODO retrieve actual rank
    }

    public CommandSource getSource() {
        return this.source;
    }

    public SlackUser getSlackUser() {
        return this.slackUser;
    }

    public User getDiscordUser() {
        return this.discordUser;
    }

    public RankTree getRank() {
        return this.rank;
    }

    public boolean getIsPrivate() {
        return this.isPrivate;
    }

    public String getChannel() {
        return this.channel;
    }

    public void sendMessage(String message) {
        switch (this.source) {
            case Slack:
                //IssueTracker.getSlack().sendMessage(message, this.channel);
                break;
            case Console:
                System.out.println(message);
                break;
            //TODO implement below methods
            case TeamSpeak:
                break;
            case Discord:
                if (getIsPrivate())
                    getDiscordUser().sendMessage(message, new MessageCallback());
                else
                    IssueTracker.getDiscord().getServer().getChannelById(getChannel()).sendMessage(message, new MessageCallback());
                break;
            default:
                break;
        }
    }

    private class MessageCallback implements FutureCallback<Message> {
        @Override
        public void onSuccess(Message message) {
            if (IssueTracker.DEBUG)
                System.out.println("[DEBUG] Message sent successfully");
        }

        @Override
        public void onFailure(Throwable t) {
            System.out.println("[ERROR] Failed to send message.");
            t.printStackTrace();
        }
    }
}