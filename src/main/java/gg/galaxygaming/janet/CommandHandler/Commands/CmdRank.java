package gg.galaxygaming.janet.CommandHandler.Commands;

import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.api.Cmd;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class CmdRank implements Cmd {
    @Override
    public void performCommand(String[] args, @Nonnull CommandSender sender) {
        sender.sendMessage(sender.getRank().getName());
    }

    @Override
    @Nonnull
    public String helpDoc() {
        return "Shows you what rank you have on slack.";
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