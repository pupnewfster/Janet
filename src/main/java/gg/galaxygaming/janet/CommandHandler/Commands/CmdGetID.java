package gg.galaxygaming.janet.CommandHandler.Commands;

import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.CommandSource;
import gg.galaxygaming.janet.CommandHandler.Rank;

import java.util.Arrays;
import java.util.List;

public class CmdGetID implements Cmd {
    @Override
    public boolean performCommand(String[] args, CommandSender sender) {
        if (sender.getSource().equals(CommandSource.TeamSpeak))
            sender.sendMessage(sender.getTeamSpeakClient().getUniqueIdentifier());
        else if (sender.getSource().equals(CommandSource.Discord)) {
            if (sender.isPrivate())
                sender.sendMessage(Long.toString(sender.getDiscordUser().getId()));
            else
                sender.sendMessage("[ERROR] This command can only be performed through a direct message to this bot.");
        }
        return true;
    }

    @Override
    public String helpDoc() {
        return "Returns your discord ID.";
    }

    @Override
    public String getUsage() {
        return "!getid";
    }

    @Override
    public String getName() {
        return "GetID";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("getdiscordid", "discordid");
    }

    @Override
    public List<CommandSource> supportedSources() {
        return Arrays.asList(CommandSource.Discord, CommandSource.TeamSpeak);
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.MEMBER;
    }
}