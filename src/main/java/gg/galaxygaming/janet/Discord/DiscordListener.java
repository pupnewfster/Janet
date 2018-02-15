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
            boolean isCommand = false;
            String m = message.getContent();
            if (m.startsWith("!")) {
                Rank rank = ((DiscordMySQL) Janet.getDiscord().getMySQL()).getRankPower(u.getRoles(Janet.getDiscord().getServer()));
                isCommand = Janet.getCommandHandler().handleCommand(m, new CommandSender(u, message.getChannel(), rank));
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