package gg.galaxygaming.janetissuetracker.CommandHandler;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.btobastian.javacord.entities.User;
import gg.galaxygaming.janetissuetracker.Discord.MessageCallback;
import gg.galaxygaming.janetissuetracker.Janet;
import gg.galaxygaming.janetissuetracker.Slack.SlackUser;

public class CommandSender {
    private CommandSource source;
    private SlackUser slackUser;
    private Client tsClient;
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

    public CommandSender(Client client) {
        this.source = CommandSource.TeamSpeak;
        this.tsClient = client;
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
            //TODO implement below methods
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