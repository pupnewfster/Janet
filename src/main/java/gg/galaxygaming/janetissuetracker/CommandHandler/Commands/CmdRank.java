package gg.galaxygaming.janetissuetracker.CommandHandler.Commands;

import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSender;
import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSource;
import gg.galaxygaming.janetissuetracker.CommandHandler.RankTree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CmdRank implements Cmd {
    @Override
    public boolean performCommand(String[] args, CommandSender sender) {
        sender.sendMessage(sender.getSlackUser().getRankName());
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
        return Arrays.asList("getrank", "slackrank");
    }

    @Override
    public List<CommandSource> supportedSources() {
        return Collections.singletonList(CommandSource.Slack);
    }

    @Override
    public RankTree getRequiredRank() {
        return RankTree.MEMBER;
    }
}