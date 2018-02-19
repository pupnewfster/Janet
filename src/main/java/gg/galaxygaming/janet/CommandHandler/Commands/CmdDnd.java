package gg.galaxygaming.janet.CommandHandler.Commands;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.CommandHandler.CommandSource;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.api.Cmd;

import java.util.Collections;
import java.util.List;

public class CmdDnd implements Cmd {
    @Override
    public void performCommand(String[] args, CommandSender sender) {
        int dnd = Janet.getTeamspeak().getDndID();
        TS3ApiAsync api = Janet.getTeamspeak().getAsyncApi();
        Client c = sender.getTeamSpeakClient();
        boolean alreadyHas = false;
        int[] serverGroups = c.getServerGroups();
        for (int id : serverGroups)
            if (id == dnd) {
                alreadyHas = true;
                break;
            }
        if (alreadyHas)
            api.removeClientFromServerGroup(dnd, c.getDatabaseId()).onSuccess(success -> sender.sendMessage("Successfully removed from DND."));
        else
            api.addClientToServerGroup(dnd, c.getDatabaseId()).onSuccess(success -> sender.sendMessage("Successfully added to DND."));
    }

    @Override
    public String helpDoc() {
        return "Toggles do not disturb.";
    }

    @Override
    public String getUsage() {
        return "!dnd";
    }

    @Override
    public String getName() {
        return "DND";
    }

    @Override
    public List<CommandSource> supportedSources() {
        return Collections.singletonList(CommandSource.TeamSpeak);
    }
}