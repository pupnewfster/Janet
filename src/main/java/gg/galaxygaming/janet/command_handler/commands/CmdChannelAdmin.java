package gg.galaxygaming.janet.command_handler.commands;

import gg.galaxygaming.janet.command_handler.CommandSender;
import gg.galaxygaming.janet.command_handler.CommandSource;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.teamspeak.TeamSpeakMySQL;
import gg.galaxygaming.janet.api.Cmd;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link Cmd} to give the {@link CommandSender} Channel Admin in their TeamSpeak room.
 */
public class CmdChannelAdmin implements Cmd {
    @Override
    public void performCommand(@Nonnull String[] args, @Nonnull CommandSender sender) {
        if (((TeamSpeakMySQL) Janet.getTeamspeak().getMySQL()).setChannelAdmin(sender.getTeamSpeakClient()))
            sender.sendMessage("Successfully given Channel Admin.");
        else
            sender.sendMessage("Failed to give Channel Admin; this is most likely because you do not have a \"User Made Room.\"");
    }

    @Override
    @Nonnull
    public String helpDoc() {
        return "Gives you Channel Admin in your \"User Made Room.\"";
    }

    @Override
    @Nonnull
    public String getUsage() {
        return "!channeladmin";
    }

    @Override
    @Nonnull
    public String getName() {
        return "ChannelAdmin";
    }

    @Override
    @Nonnull
    public List<String> getAliases() {
        return Collections.singletonList("ca");
    }

    @Override
    public List<CommandSource> supportedSources() {
        return Collections.singletonList(CommandSource.TeamSpeak);
    }
}