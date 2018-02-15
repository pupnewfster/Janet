package gg.galaxygaming.janet.Discord;

import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.events.message.MessageCreateEvent;
import de.btobastian.javacord.events.server.member.ServerMemberJoinEvent;
import de.btobastian.javacord.listeners.message.MessageCreateListener;
import de.btobastian.javacord.listeners.server.member.ServerMemberJoinListener;
import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.Rank;
import gg.galaxygaming.janet.Janet;

public class DiscordListener implements MessageCreateListener, ServerMemberJoinListener {
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
                    Janet.getSlack().sendMessage("From Discord - " + u.getDisplayName(discord.getServer()) + ": " + m, Janet.getSlack().getInfoChannel());
            }
        });
    }

    @Override
    public void onServerMemberJoin(ServerMemberJoinEvent event) {
        if (Janet.getDiscord().getServer().equals(event.getServer())) {
            event.getUser().sendMessage(Janet.getDiscord().getAuthMessage());
        }
    }
}