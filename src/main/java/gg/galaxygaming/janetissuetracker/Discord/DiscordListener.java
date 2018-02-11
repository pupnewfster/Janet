package gg.galaxygaming.janetissuetracker.Discord;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSender;
import gg.galaxygaming.janetissuetracker.IssueTracker;

public class DiscordListener implements MessageCreateListener {
    @Override
    public void onMessageCreate(DiscordAPI api, Message message) {
        String m = message.getContent();
        boolean isCommand = false;
        CommandSender sender = message.isPrivateMessage() ? new CommandSender(message.getAuthor(), "", true) : new CommandSender(message.getAuthor(), message.getChannelReceiver().getId(), false);
        if (m.startsWith("!"))
            isCommand = IssueTracker.getCommandHandler().handleCommand(m, sender);
    }
}