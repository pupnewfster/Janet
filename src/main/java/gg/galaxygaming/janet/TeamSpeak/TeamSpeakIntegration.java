package gg.galaxygaming.janet.TeamSpeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.api.AbstractIntegration;

import javax.annotation.Nonnull;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.Integration} to connect to the TeamSpeak server.
 */
public class TeamSpeakIntegration extends AbstractIntegration {//TODO AutoReconnect if teamspeak server goes down or restarts
    private final int dndID, verifiedID, defaultChannel;
    private final String joinMessage, verifyMessage, roomCreatorName;
    private TS3Config ts3Config;
    private TS3Query ts3Query;
    private TS3ApiAsync asyncApi;
    private TeamSpeakListener listener;

    public TeamSpeakIntegration() {
        super();
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
            Janet.getLogger().error("Failed to load needed configs for TeamSpeak Integration");
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

    private void login(@Nonnull String username, @Nonnull String password) {
        getAsyncApi().login(username, password).onSuccess(loggedIn -> {
            if (loggedIn)
                selectServer();
            else
                Janet.getLogger().error("Failed to authenticate TeamSpeak Query.");
        });
    }

    private void selectServer() {
        getAsyncApi().selectVirtualServerById(1).onSuccess(serverSelected -> {
            if (serverSelected)
                setNickName();
            else
                Janet.getLogger().error("Failed to select TeamSpeak server.");
        });
    }

    private void setNickName() {
        getAsyncApi().setNickname("Janet").onSuccess(nickSet -> {
            if (nickSet)
                findDefaultChannel();
            else
                Janet.getLogger().error("Failed to set TeamSpeak nickname.");
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

        //getAsyncApi().getClients().onSuccess(clients -> clients.stream().filter(Client::isRegularClient).forEach(this::checkVerification));

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

    /**
     * Retrieves the TeamSpeak rank to give when a user toggles DND.
     * @return The TeamSpeak rank to give when a user toggles DND.
     */
    public int getDndID() {
        return this.dndID;
    }

    /**
     * Retrieves the TeamSpeak rank to give when a user verifies.
     * @return The TeamSpeak rank to give when a user verifies.
     */
    public int getVerifiedID() {
        return this.verifiedID;
    }

    /**
     * Retrieves the name that will be used to check if a room should be automatically generated.
     * @return The name that will be used to check if a room should be automatically generated.
     */
    @Nonnull
    public String getRoomCreatorName() {
        return this.roomCreatorName;
    }

    /**
     * Retrieves the {@link TS3ApiAsync} instance of this {@link TeamSpeakIntegration}.
     * @return The {@link TS3ApiAsync} instance of this {@link TeamSpeakIntegration}.
     */
    public TS3ApiAsync getAsyncApi() {
        return this.asyncApi;
    }

    /**
     * Retrieves the ID of the default channel.
     * @return The ID of the default channel.
     */
    public int getDefaultChannelID() {
        return this.defaultChannel;
    }

    /**
     * Checks if the specified {@link Client} has verified, and if not send them a message telling them how to verify.
     * <p>
     * This also sends them a message every time they join so that they can message {@link gg.galaxygaming.janet.Janet} commands.
     * @param c The {@link Client} to check the verification status of.
     */
    public void checkVerification(@Nonnull Client c) {
        getAsyncApi().sendPrivateMessage(c.getId(), joinMessage).onSuccess(sent -> {
            if (sent) {
                int[] serverGroups = c.getServerGroups();
                boolean verified = false;
                for (int id : serverGroups)
                    if (id == verifiedID) {
                        verified = true;
                        break;
                    }
                if (!verified)
                    getAsyncApi().sendPrivateMessage(c.getId(), verifyMessage);
            } else
                Janet.getLogger().debug("Failed to send message to " + c.getNickname() + '.');
        });
    }
}