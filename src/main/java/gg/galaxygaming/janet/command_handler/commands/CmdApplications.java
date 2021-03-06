package gg.galaxygaming.janet.command_handler.commands;

import gg.galaxygaming.janet.command_handler.CommandSender;
import gg.galaxygaming.janet.command_handler.CommandSource;
import gg.galaxygaming.janet.command_handler.Rank;
import gg.galaxygaming.janet.forums.ForumMySQL;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.api.Cmd;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link Cmd} to list the open applications on the forums.
 */
public class CmdApplications implements Cmd {
    @Override
    public void performCommand(@Nonnull String[] args, @Nonnull CommandSender sender) {
        if (args.length == 0) {
            sender.sendMessage("ERROR: You must input a server to view the open applications of.");
            return;
        }
        Map<Integer, String> applicationNames = ((ForumMySQL) Janet.getForums().getMySQL()).getApplicationNames(args[0]);
        if (applicationNames == null) {
            sender.sendMessage("Unknown server: " + args[0]);
            return;
        }
        if (applicationNames.isEmpty()) {
            sender.sendMessage("No applications for server: " + args[0]);
            return;
        }
        Set<Map.Entry<Integer, String>> entries = applicationNames.entrySet();
        StringBuilder m = new StringBuilder("ID. NAME, Created By");
        for (Map.Entry<Integer, String> entry : entries)
            m.append('\n').append(entry.getKey()).append(". ").append(entry.getValue());
        sender.sendMessage(m.toString());
    }

    @Override
    @Nonnull
    public String helpDoc() {
        return "Displays the names and ids of the open applications for the given <server>.";
    }

    @Override
    @Nonnull
    public String getUsage() {
        return "!applications <server>";
    }

    @Override
    @Nonnull
    public String getName() {
        return "Applications";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("apps", "getapps", "getapplications", "forums", "getforums");
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