package gg.galaxygaming.janet.command_handler.commands;

import gg.galaxygaming.janet.command_handler.CommandSender;
import gg.galaxygaming.janet.command_handler.CommandSource;
import gg.galaxygaming.janet.command_handler.Rank;
import gg.galaxygaming.janet.forums.ForumMySQL;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.api.Cmd;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * An implementation of {@link Cmd} to accept an open applications on the forums.
 */
public class CmdAccept implements Cmd {
    @Override
    public void performCommand(@Nonnull String[] args, @Nonnull CommandSender sender) {
        if (args.length == 0 || !Utils.legalInt(args[0]))
            sender.sendMessage("ERROR: You must input the id of the application you are accepting. You can use !applications to view the list of open applications.");
        else
            sender.sendMessage(((ForumMySQL) Janet.getForums().getMySQL()).acceptApp(Integer.parseInt(args[0])));
    }

    @Override
    @Nonnull
    public String helpDoc() {
        return "Accepts the application with the given <topic id> on the forums.";
    }

    @Override
    @Nonnull
    public String getUsage() {
        return "!accept <topic id>";
    }

    @Override
    @Nonnull
    public String getName() {
        return "Accept";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("acceptapp", "acceptapplication");
    }

    @Nullable
    public List<CommandSource> supportedSources() {
        return Arrays.asList(CommandSource.Discord, CommandSource.TeamSpeak, CommandSource.Console);
    }

    @Nonnull
    public Rank getRequiredRank() {
        return Rank.MANAGER;
    }
}