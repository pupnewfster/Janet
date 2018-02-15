package gg.galaxygaming.janet.CommandHandler.Commands;

import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.CommandSource;
import gg.galaxygaming.janet.CommandHandler.Rank;

import java.util.Collections;
import java.util.List;

public class CmdRank implements Cmd {
    @Override
    public boolean performCommand(String[] args, CommandSender sender) {
        sender.sendMessage(sender.getRank().getName());
        return true;
    }

    @Override
    public String helpDoc() {
        return "Shows you what rank you have on slack.";
    }

    @Override
    public String getUsage() {
        return "!rank";
    }

    @Override
    public String getName() {
        return "Rank";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("getrank");
    }

    @Override
    public List<CommandSource> supportedSources() {
        return null;
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.MEMBER;
    }
}