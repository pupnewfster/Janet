package gg.galaxygaming.janetissuetracker.CommandHandler.Commands;

import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSender;
import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSource;
import gg.galaxygaming.janetissuetracker.CommandHandler.RankTree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CmdGetID implements Cmd {
    @Override
    public boolean performCommand(String[] args, CommandSender sender) {
        if (sender.getIsPrivate())
            sender.sendMessage(sender.getDiscordUser().getId());
        else
            sender.sendMessage("[ERROR] This command can only be performed through a direct message to this bot.");
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
        return Collections.singletonList(CommandSource.Discord);
    }

    @Override
    public RankTree getRequiredRank() {
        return RankTree.MEMBER;
    }
}