package gg.galaxygaming.janetissuetracker.TeamSpeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSender;
import gg.galaxygaming.janetissuetracker.Janet;
import gg.galaxygaming.janetissuetracker.Utils;

import java.util.HashMap;

public class TeamSpeakListener extends TS3EventAdapter {
    @Override
    public void onTextMessage(TextMessageEvent e) {
        if (e.getTargetMode() == TextMessageTargetMode.CLIENT) {
            Janet.getTeamspeak().getAsyncApi().getClientInfo(e.getInvokerId()).onSuccess(clientInfo -> {
                if (clientInfo != null && clientInfo.isRegularClient()) {
                    String m = e.getMessage();
                    boolean isCommand = false;
                    if (m.startsWith("!"))
                        isCommand = Janet.getCommandHandler().handleCommand(m, new CommandSender(clientInfo));
                }
            });
        }
    }

    @Override
    public void onClientJoin(ClientJoinEvent e) {
        Janet.getTeamspeak().getAsyncApi().getClientInfo(e.getClientId()).onSuccess(clientInfo -> {
            if (clientInfo != null && clientInfo.isRegularClient()) {
                Janet.getTeamspeak().checkVerification(clientInfo);
                handleRoomCreation(clientInfo.getChannelId(), clientInfo.getId());
                Janet.getTeamspeak().getMySQL().check(clientInfo);
            }
        });
    }

    @Override
    public void onClientMoved(ClientMovedEvent e) {
        Janet.getTeamspeak().getAsyncApi().getClientInfo(e.getClientId()).onSuccess(clientInfo -> {
            if (clientInfo != null && clientInfo.isRegularClient())
                handleRoomCreation(clientInfo.getChannelId(), clientInfo.getId());
        });
    }

    private void handleRoomCreation(int cid, int clientID) {
        TeamSpeakIntegration ts = Janet.getTeamspeak();
        TS3ApiAsync api = ts.getAsyncApi();
        api.getChannelInfo(cid).onSuccess(channelInfo -> {
            if (channelInfo.getName().equalsIgnoreCase(ts.getRoomCreatorName())) {
                int pid = channelInfo.getParentChannelId();
                api.getChannelInfo(pid).onSuccess(pInfo -> {
                    if (pInfo.getMaxClients() > 0 || pInfo.getName().startsWith("[cspacer"))
                        return;
                    api.getChannels().onSuccess(channels -> {
                        String name, sNum;
                        int lastNum = 0, num, bcid = cid;
                        for (Channel c : channels)
                            if (c.getParentChannelId() == pid) {
                                name = c.getName();
                                if (!name.startsWith("Room "))
                                    continue;
                                sNum = name.replaceFirst("Room ", "");
                                if (Utils.legalInt(sNum)) {
                                    num = Integer.parseInt(sNum);
                                    if (num - lastNum != 1)
                                        break;
                                    lastNum = num;
                                }
                                bcid = c.getId();
                            }
                        final HashMap<ChannelProperty, String> properties = new HashMap<>();
                        properties.put(ChannelProperty.CPID, Integer.toString(pid));
                        properties.put(ChannelProperty.CHANNEL_DESCRIPTION, "Janet generated " + pInfo.getName() + " channel");
                        properties.put(ChannelProperty.CHANNEL_ORDER, Integer.toString(bcid));
                        api.createChannel("Room " + (lastNum + 1), properties).onSuccess(cID -> api.moveClient(clientID, cID).onSuccess(success ->
                                api.moveQuery(ts.getDefaultChannelID())));
                    });
                });
            }
        });
    }
}