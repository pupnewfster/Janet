package gg.galaxygaming.janet.api;

import gg.galaxygaming.janet.command_handler.CommandSender;
import gg.galaxygaming.janet.command_handler.CommandSource;
import gg.galaxygaming.janet.command_handler.Rank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * The base interface of all Commands that {@link gg.galaxygaming.janet.Janet} implements.
 */
public interface Cmd {//TODO should this be moved to the api package?

    /**
     * Performs the logic behind {@link Cmd}.
     * @param args A List of the arguments passed to this {@link Cmd}.
     * @param info The {@link CommandSender} that performed this {@link Cmd}.
     */
    void performCommand(@Nonnull String[] args, @Nonnull CommandSender info);

    /**
     * Retrieves help documentation for this {@link Cmd}. This shows up in when using {@link gg.galaxygaming.janet.command_handler.commands.CmdHelp}.
     * @return The help documentation for this {@link Cmd}.
     */
    @Nonnull
    String helpDoc();

    /**
     * Retrieves the proper usage for this {@link Cmd}. This shows up in when using {@link gg.galaxygaming.janet.command_handler.commands.CmdHelp}.
     * @return The proper usage for this {@link Cmd}.
     */
    @Nonnull
    String getUsage();//TODO: Improve and add a way to show examples

    /**
     * Retrieves the proper Name for this {@link Cmd}.
     * @return The proper Name for this {@link Cmd}.
     */
    @Nonnull
    String getName();

    /**
     * Retrieves a {@link List} of all supported aliases, or null if there are no aliases for this {@link Cmd}
     * @return A {@link List} of all supported aliases for this {@link Cmd}, or null if there are no aliases.
     */
    @Nullable
    default List<String> getAliases() {
        return null;
    }

    /**
     * Retrieves a {@link List} of all supported {@link CommandSource}s, or null if it the {@link Cmd} supports all {@link CommandSource}s.
     * @return A {@link List} composed of {@link CommandSource}, or null if it supports all sources.
     */
    @Nullable
    default List<CommandSource> supportedSources() {
        return null;
    }

    /**
     * Retrieves the minimum {@link Rank} required to perform this command. Defaults to {@link Rank#GUEST}.
     * @return The required {@link Rank} to use this command.
     */
    @Nonnull
    default Rank getRequiredRank() {
        return Rank.GUEST;
    }
}