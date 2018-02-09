package gg.galaxygaming.janetissuetracker.CommandHandler.Commands;

import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSender;
import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSource;
import gg.galaxygaming.janetissuetracker.CommandHandler.RankTree;

import java.util.List;

public interface Cmd {
    @SuppressWarnings("SameReturnValue")
    boolean performCommand(String[] args, CommandSender info);

    String helpDoc();

    String getUsage();//TODO: Improve and add a way to show examples

    String getName();

    List<String> getAliases();

    List<CommandSource> supportedSources();

    RankTree getRequiredRank();
}