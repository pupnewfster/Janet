package gg.galaxygaming.janet.CommandHandler;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Slack.SlackIntegration;
import gg.galaxygaming.janet.Slack.SlackUser;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.user.User;

import javax.annotation.Nonnull;

/**
 * Stores general information about who sent a {@link gg.galaxygaming.janet.api.Cmd}.
 */
public final class CommandSender {
    private final CommandSource source;
    private final Rank rank;
    private SlackUser slackUser;
    private Client tsClient;
    private User discordUser;
    private String channel;
    private boolean isPrivate;
    private TextChannel dChannel;

    /**
     * Creates a Console instance of {@link CommandSender}.
     */
    public CommandSender() { //TODO should console command sender be static, probably
        this.source = CommandSource.Console;
        this.rank = Rank.EXECUTIVE_STAFF;
    }

    /**
     * Creates a Slack instance of {@link CommandSender}.
     * @param user    The {@link SlackUser} behind the {@link CommandSender}.
     * @param channel The ID of the Slack channel the {@link SlackUser} is in.
     */
    public CommandSender(SlackUser user, String channel) {
        this.source = CommandSource.Slack;
        this.slackUser = user;
        this.channel = channel;
        this.isPrivate = this.channel.startsWith("D");
        this.rank = this.slackUser.getRank();
    }

    /**
     * Creates a Discord instance of {@link CommandSender}.
     * @param user    The {@link User} behind the {@link CommandSender}.
     * @param channel The {@link TextChannel} that the {@link User} is in.
     * @param rank    The {@link Rank} the {@link User} has.
     */
    public CommandSender(User user, TextChannel channel, Rank rank) {
        this.source = CommandSource.Discord;
        this.discordUser = user;
        this.dChannel = channel;
        this.isPrivate = this.dChannel instanceof PrivateChannel;
        this.rank = rank;
    }

    /**
     * Creates a TeamSpeak instance of {@link CommandSender}.
     * @param client The {@link Client} behind the {@link CommandSender}.
     * @param rank   The {@link Rank} the {@link Client} has.
     */
    public CommandSender(Client client, Rank rank) {
        this.source = CommandSource.TeamSpeak;
        this.tsClient = client;
        this.isPrivate = true;//TODO: If we end up supporting non pms again on TeamSpeak update this to reflect that
        this.rank = rank;
    }

    /**
     * Retrieves the {@link CommandSource} of {@link CommandSender} object.
     * @return The {@link CommandSource} of {@link CommandSender} object.
     */
    public CommandSource getSource() {
        return this.source;
    }

    /**
     * If the {@link CommandSource} is {@link CommandSource#Slack} then it returns the {@link SlackUser} behind this {@link CommandSender}.
     * @return The {@link SlackUser} behind this {@link CommandSender}.
     */
    public SlackUser getSlackUser() {
        return this.slackUser;
    }

    /**
     * If the {@link CommandSource} is {@link CommandSource#Discord} then it returns the {@link User} behind this {@link CommandSender}.
     * @return The {@link User} behind this {@link CommandSender}.
     */
    public User getDiscordUser() {
        return this.discordUser;
    }

    /**
     * If the {@link CommandSource} is {@link CommandSource#TeamSpeak} then it returns the {@link Client} behind this {@link CommandSender}.
     * @return The {@link Client} behind this {@link CommandSender}.
     */
    public Client getTeamSpeakClient() {
        return this.tsClient;
    }

    /**
     * Retrieves the {@link Rank} of the {@link CommandSender}.
     * @return The {@link Rank} of the {@link CommandSender}.
     */
    public Rank getRank() {
        return this.rank;
    }

    /**
     * Gets if the {@link CommandSender} is in a direct message with {@link gg.galaxygaming.janet.Janet}.
     * @return True if the message was a private message to {@link gg.galaxygaming.janet.Janet}, false otherwise.
     */
    public boolean isPrivate() {
        return this.isPrivate;
    }

    /**
     * Gets the Channel ID of the Slack channel the message was sent in.
     * @return The Slack Channel ID.
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sends a message to the {@link CommandSender}.
     * @param message The message to send.
     */
    public void sendMessage(@Nonnull String message) {
        switch (this.source) {
            case Slack:
                SlackIntegration slack = Janet.getSlack();
                if (slack != null) { //Is unlikely to ever be the case
                    slack.sendMessage(message, this.channel);
                }
                break;
            case Console:
                Janet.getLogger().info(message);
                break;
            case TeamSpeak:
                Janet.getTeamspeak().getAsyncApi().sendPrivateMessage(this.tsClient.getId(), message);
                break;
            case Discord:
                this.dChannel.sendMessage(message);
                break;
            default:
                break;
        }
    }
}