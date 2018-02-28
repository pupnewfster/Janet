package gg.galaxygaming.janet.Discord;

import de.btobastian.javacord.entity.message.Message;
import de.btobastian.javacord.event.message.MessageCreateEvent;
import de.btobastian.javacord.event.server.member.ServerMemberJoinEvent;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import de.btobastian.javacord.listener.server.member.ServerMemberJoinListener;
import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Janet;

/**
 * A listener to listen to events that happen on Discord.
 */
public class DiscordListener implements MessageCreateListener, ServerMemberJoinListener {//TODO: listen to message edit events/delete and pass it on to slack??

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
            String m = message.getReadableContent();
            DiscordIntegration discord = Janet.getDiscord();
            if (m.startsWith("!")) {
                Rank rank = ((DiscordMySQL) discord.getMySQL()).getRankPower(u.getRoles(discord.getServer()));
                isCommand = Janet.getCommandHandler().handleCommand(m, new CommandSender(u, message.getChannel(), rank));
            }
            if (!isCommand) {
                if (message.getChannel().getId() == discord.getDevChannel())
                    Janet.getSlack().sendMessage(u.getDisplayName(discord.getServer()) + ": " + m, Janet.getSlack().getInfoChannel());
            }
        });
    }

    /**
     * Called when a new member joins the Discord server.
     */
    @Override
    public void onServerMemberJoin(ServerMemberJoinEvent event) {
        if (Janet.getDiscord().getServer().equals(event.getServer())) {
            event.getUser().sendMessage(Janet.getDiscord().getAuthMessage());
        }
    }
}