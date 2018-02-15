package gg.galaxygaming.janet.CommandHandler.Commands;

import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.CommandSource;
import gg.galaxygaming.janet.CommandHandler.Rank;

import java.util.List;

public interface Cmd {
    @SuppressWarnings("SameReturnValue")
    boolean performCommand(String[] args, CommandSender info);

    String helpDoc();

    String getUsage();//TODO: Improve and add a way to show examples

    String getName();

    List<String> getAliases();

    List<CommandSource> supportedSources();

    Rank getRequiredRank();
}