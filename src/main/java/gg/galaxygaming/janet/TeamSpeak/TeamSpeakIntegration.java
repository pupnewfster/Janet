package gg.galaxygaming.janet.TeamSpeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.base.AbstractIntegration;

public class TeamSpeakIntegration extends AbstractIntegration {//TODO AutoReconnect if teamspeak server goes down or restarts
    private final int dndID, verifiedID, defaultChannel;
    private final String joinMessage, verifyMessage, roomCreatorName;
    private TS3Config ts3Config;
    private TS3Query ts3Query;
    private TS3ApiAsync asyncApi;
    private TeamSpeakListener listener;

    public TeamSpeakIntegration() {
        Config config = Janet.getConfig();
        String username = config.getStringOrDefault("TEAMSPEAK_USERNAME", "username");
        String password = config.getStringOrDefault("TEAMSPEAK_PASSWORD", "password");
        this.dndID = config.getIntegerOrDefault("TEAMSPEAK_DND", -1);
        this.verifiedID = config.getIntegerOrDefault("TEAMSPEAK_VERIFIED", -1);
        this.defaultChannel = config.getIntegerOrDefault("TEAMSPEAK_DEFAULT", -1);
        this.joinMessage = config.getStringOrDefault("TEAMSPEAK_JOIN", "Welcome.");
        this.verifyMessage = config.getStringOrDefault("TEAMSPEAK_VERIFY", "Go verify.");
        this.roomCreatorName = config.getStringOrDefault("TEAMSPEAK_ROOM_CREATOR", "Join here to create a new room");
        if (username.equals("username") || password.equals("password") || this.dndID < 0 || this.verifiedID < 0 || this.defaultChannel < 0) {
            System.out.println("[ERROR] Failed to load needed configs for TeamSpeak Integration");
            return;
        }
        this.ts3Config = new TS3Config();
        this.ts3Config.setHost(config.getStringOrDefault("TEAMSPEAK_HOST", "127.0.0.1"));
        //this.ts3Config.setDebugLevel(Level.OFF);
        this.ts3Config.setFloodRate(TS3Query.FloodRate.UNLIMITED);

        this.ts3Query = new TS3Query(this.ts3Config);
        this.ts3Query.connect();

        this.asyncApi = this.ts3Query.getAsyncApi();
        login(username, password);
    }

    private void login(String username, String password) {
        getAsyncApi().login(username, password).onSuccess(loggedIn -> {
            if (loggedIn)
                selectServer();
            else
                System.out.println("[ERROR] Failed to authenticate TeamSpeak Query.");
        });
    }

    private void selectServer() {
        getAsyncApi().selectVirtualServerById(1).onSuccess(serverSelected -> {
            if (serverSelected)
                setNickName();
            else
                System.out.println("[ERROR] Failed to select TeamSpeak server.");
        });
    }

    private void setNickName() {
        getAsyncApi().setNickname("Janet").onSuccess(nickSet -> {
            if (nickSet)
                findDefaultChannel();
            else
                System.out.println("[ERROR] Failed to set nickname.");
        });
    }

    private void findDefaultChannel() {
        getAsyncApi().whoAmI().onSuccess(info -> {
            if (info != null)
                finishConnect();
        });
    }

    private void finishConnect() {
        this.listener = new TeamSpeakListener();
        getAsyncApi().registerAllEvents();
        getAsyncApi().addTS3Listeners(this.listener);

        getAsyncApi().getClients().onSuccess(clients -> clients.stream().filter(Client::isRegularClient).forEach(this::checkVerification));

        this.mysql = new TeamSpeakMySQL();
    }

    public void stop() {
        super.stop();
        if (getAsyncApi() != null) {
            if (this.listener != null)
                getAsyncApi().removeTS3Listeners(this.listener);
            getAsyncApi().logout();
        }
        this.ts3Query.exit();
    }

    public int getDndID() {
        return this.dndID;
    }

    public int getVerifiedID() {
        return this.verifiedID;
    }

    public String getVerifyMessage() {
        return this.verifyMessage;
    }

    public String getJoinMessage() {
        return this.joinMessage;
    }

    public String getRoomCreatorName() {
        return this.roomCreatorName;
    }

    public TS3ApiAsync getAsyncApi() {
        return this.asyncApi;
    }

    public int getDefaultChannelID() {
        return this.defaultChannel;
    }

    public void checkVerification(Client c) {
        getAsyncApi().sendPrivateMessage(c.getId(), getJoinMessage()).onSuccess(sent -> {
            if (sent) {
                int[] serverGroups = c.getServerGroups();
                boolean verified = false;
                for (int id : serverGroups)
                    if (id == verifiedID) {
                        verified = true;
                        break;
                    }
                if (!verified)
                    getAsyncApi().sendPrivateMessage(c.getId(), getVerifyMessage());
            } else if (Janet.DEBUG)
                System.out.println("[DEBUG] Failed to send message.");
        });
    }
}