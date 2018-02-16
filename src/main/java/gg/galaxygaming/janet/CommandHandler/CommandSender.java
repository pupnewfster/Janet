package gg.galaxygaming.janet.CommandHandler;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.channels.PrivateChannel;
import de.btobastian.javacord.entities.channels.TextChannel;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Slack.SlackUser;

public class CommandSender {
    private CommandSource source;
    private SlackUser slackUser;
    private Client tsClient;
    private User discordUser;
    private String channel;
    private Rank rank;
    private boolean isPrivate;
    private TextChannel dChannel;

    public CommandSender() {
        this.source = CommandSource.Console;
    }

    public CommandSender(SlackUser user, String channel) {
        this.source = CommandSource.Slack;
        this.slackUser = user;
        this.channel = channel;
        this.rank = this.slackUser.getRank();
    }

    public CommandSender(User user, TextChannel channel) {
        this(user, channel, Rank.MEMBER);
    }

    public CommandSender(User user, TextChannel channel, Rank rank) {
        this.source = CommandSource.Discord;
        this.discordUser = user;
        this.dChannel = channel;
        this.isPrivate = this.dChannel instanceof PrivateChannel;
        this.rank = rank;//TODO retrieve actual rank
    }

    public CommandSender(Client client) {
        this(client, Rank.MEMBER);
    }

    public CommandSender(Client client, Rank rank) {
        this.source = CommandSource.TeamSpeak;
        this.tsClient = client;
        this.rank = rank;
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

    public Client getTeamSpeakClient() {
        return this.tsClient;
    }

    public Rank getRank() {
        return this.rank;
    }

    public boolean isPrivate() {
        return this.isPrivate;
    }

    public String getChannel() {
        return this.channel;
    }

    public void sendMessage(String message) {
        switch (this.source) {
            case Slack:
                Janet.getSlack().sendMessage(message, this.channel);
                break;
            case Console:
                Janet.getLogger().info(message);
                break;
            case TeamSpeak:
                Janet.getTeamspeak().getAsyncApi().sendPrivateMessage(getTeamSpeakClient().getId(), message);
                break;
            case Discord:
                this.dChannel.sendMessage(message);
                break;
            default:
                break;
        }
    }
}