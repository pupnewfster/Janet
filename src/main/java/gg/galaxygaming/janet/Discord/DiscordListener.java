package gg.galaxygaming.janet.Discord;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import de.btobastian.javacord.listener.server.ServerMemberAddListener;
import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.RankTree;
import gg.galaxygaming.janet.Janet;

public class DiscordListener implements MessageCreateListener, ServerMemberAddListener {
    @Override
    public void onMessageCreate(DiscordAPI api, Message message) {
        String m = message.getContent();
        boolean isCommand = false;
        if (m.startsWith("!")) {
            RankTree rank = RankTree.MEMBER;//TODO set
            CommandSender sender = message.isPrivateMessage() ? new CommandSender(message.getAuthor(), "", true, rank) : new CommandSender(message.getAuthor(), message.getChannelReceiver().getId(), false, rank);
            isCommand = Janet.getCommandHandler().handleCommand(m, sender);
        }
    }

    @Override
    public void onServerMemberAdd(DiscordAPI api, User user, Server server) {
        if (Janet.getDiscord().getServer().equals(server))
            user.sendMessage(Janet.getDiscord().getAuthMessage(), new MessageCallback());
    }
}