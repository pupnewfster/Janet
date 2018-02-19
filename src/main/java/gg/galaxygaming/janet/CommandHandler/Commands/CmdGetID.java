package gg.galaxygaming.janet.CommandHandler.Commands;

import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.CommandSource;
import gg.galaxygaming.janet.api.Cmd;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class CmdGetID implements Cmd {
    @Override
    public void performCommand(String[] args, @Nonnull CommandSender sender) {
        if (sender.getSource().equals(CommandSource.TeamSpeak))
            sender.sendMessage(sender.getTeamSpeakClient().getUniqueIdentifier());
        else if (sender.getSource().equals(CommandSource.Discord)) {
            if (sender.isPrivate())
                sender.sendMessage(Long.toString(sender.getDiscordUser().getId()));
            else
                sender.sendMessage("[ERROR] This command can only be performed through a direct message to this bot.");
        }
    }

    @Override
    @Nonnull
    public String helpDoc() {
        return "Returns your discord ID.";
    }

    @Override
    @Nonnull
    public String getUsage() {
        return "!getid";
    }

    @Override
    @Nonnull
    public String getName() {
        return "GetID";
    }

    @Override
    public List<CommandSource> supportedSources() {
        return Arrays.asList(CommandSource.Discord, CommandSource.TeamSpeak);
    }
}