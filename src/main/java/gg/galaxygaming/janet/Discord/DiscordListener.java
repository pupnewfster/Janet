package gg.galaxygaming.janet.Discord;

import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Janet;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.MessageEditListener;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;

import java.util.Collection;

/**
 * A listener to listen to events that happen on Discord.
 */
public class DiscordListener implements MessageCreateListener, ServerMemberJoinListener, MessageEditListener, MessageDeleteListener {
    /**
     * Called when a message is sent on Discord.
     */
    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();
        message.getAuthor().asUser().ifPresent(u -> {
            if (u.isBot())
                return;
            boolean isCommand = false;
            String m = message.getReadableContent().trim();
            DiscordIntegration discord = Janet.getDiscord();
            if (m.startsWith("!")) {
                Rank rank = ((DiscordMySQL) discord.getMySQL()).getRankPower(u.getRoles(discord.getServer()));
                isCommand = Janet.getCommandHandler().handleCommand(m, new CommandSender(u, message.getChannel(), rank));
            }
            if (!isCommand) {
                if (message.getChannel().getId() == discord.getDevChannel() && !m.isEmpty())
                    Janet.getSlack().sendMessage(u.getDisplayName(discord.getServer()) + ": " + m, Janet.getSlack().getInfoChannel());
            }
            if (message.getChannel().getId() == discord.getDevChannel()) {
                message.getAttachments().forEach(attachment -> Janet.getSlack().sendMessage(u.getDisplayName(discord.getServer()) + " attached " +
                        attachment.getUrl().toString(), Janet.getSlack().getInfoChannel()));
            }
        });
    }

    /**
     * Called when a message is edited on Discord.
     */
    @Override
    public void onMessageEdit(MessageEditEvent messageEditEvent) {

    }

    /**
     * Called when a message is deleted on Discord.
     */
    @Override
    public void onMessageDelete(MessageDeleteEvent messageDeleteEvent) {

    }

    /**
     * Called when a new member joins the Discord server.
     */
    @Override
    public void onServerMemberJoin(ServerMemberJoinEvent event) {
        if (Janet.getDiscord().getServer().equals(event.getServer())) {
            event.getUser().sendMessage(Janet.getDiscord().getAuthMessage());
        }
        Collection<User> cachedUsers = Janet.getDiscord().getApi().getCachedUsers();
        if (!cachedUsers.contains(event.getUser()))
            cachedUsers.add(event.getUser());
    }
}