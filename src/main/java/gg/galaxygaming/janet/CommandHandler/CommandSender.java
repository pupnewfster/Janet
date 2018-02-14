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
    private RankTree rank;
    private boolean isPrivate;
    private TextChannel dChannel;

    //TODO create a console command sender

    public CommandSender(SlackUser user, String channel) {
        this.source = CommandSource.Slack;
        this.slackUser = user;
        this.channel = channel;
        this.rank = this.slackUser.getRank();
    }

    public CommandSender(User user, TextChannel channel) {
        this(user, channel, RankTree.MEMBER);
    }

    public CommandSender(User user, TextChannel channel, RankTree rank) {
        this.source = CommandSource.Discord;
        this.discordUser = user;
        this.dChannel = channel;
        this.isPrivate = this.dChannel instanceof PrivateChannel;
        this.rank = rank;//TODO retrieve actual rank
    }

    public CommandSender(Client client) {
        this(client, RankTree.MEMBER);
    }

    public CommandSender(Client client, RankTree rank) {
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

    public RankTree getRank() {
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
                //Janet.getSlack().sendMessage(message, this.channel);
                break;
            case Console:
                System.out.println(message);
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