package gg.galaxygaming.janet.command_handler.commands;

import gg.galaxygaming.janet.command_handler.CommandSender;
import gg.galaxygaming.janet.api.Cmd;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link Cmd} to return the {@link gg.galaxygaming.janet.command_handler.Rank} the {@link CommandSender} has.
 */
public class CmdRank implements Cmd {
    @Override
    public void performCommand(@Nonnull String[] args, @Nonnull CommandSender sender) {
        sender.sendMessage(sender.getRank().getName());
    }

    @Override
    @Nonnull
    public String helpDoc() {
        return "Shows you what rank you have.";
    }

    @Override
    @Nonnull
    public String getUsage() {
        return "!rank";
    }

    @Override
    @Nonnull
    public String getName() {
        return "Rank";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("getrank");
    }
}