package gg.galaxygaming.janet.CommandHandler;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.btobastian.javacord.entities.User;
import gg.galaxygaming.janet.Discord.MessageCallback;
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

    //TODO create a console command sender

    public CommandSender(SlackUser user, String channel) {
        this.source = CommandSource.Slack;
        this.slackUser = user;
        this.channel = channel;
        this.rank = this.slackUser.getRank();
    }

    public CommandSender(User user, String channel) {
        this(user, channel, false, RankTree.MEMBER);
    }

    public CommandSender(User user, String channel, RankTree rank) {
        this(user, channel, false, rank);
    }

    public CommandSender(User user, String channel, boolean isPrivate) {
        this(user, channel, isPrivate, RankTree.MEMBER);
    }

    public CommandSender(User user, String channel, boolean isPrivate, RankTree rank) {
        this.source = CommandSource.Discord;
        this.discordUser = user;
        this.channel = channel;
        this.isPrivate = isPrivate;
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

    public boolean getIsPrivate() {
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
                if (getIsPrivate())
                    getDiscordUser().sendMessage(message, new MessageCallback());
                else
                    Janet.getDiscord().getServer().getChannelById(getChannel()).sendMessage(message, new MessageCallback());
                break;
            default:
                break;
        }
    }
}