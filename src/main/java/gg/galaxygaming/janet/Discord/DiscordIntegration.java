package gg.galaxygaming.janet.Discord;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Server;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.base.AbstractIntegration;

public class DiscordIntegration extends AbstractIntegration {
    private final String token;
    private final String serverID;
    private final String authMessage;
    private DiscordAPI api;
    private DiscordListener listeners;
    private Server server;

    public DiscordIntegration() {
        Config config = Janet.getConfig();
        this.token = config.getStringOrDefault("DISCORD_TOKEN", "token");
        this.serverID = config.getStringOrDefault("DISCORD_SERVER", "server");
        this.authMessage = config.getStringOrDefault("DISCORD_AUTH_MESSAGE", "Go authenticate your account.");
        if (this.token.equals("token") || this.serverID.equals("server")) {
            System.out.println("[ERROR] Failed to load needed configs for Discord Integration");
            return;
        }
        this.api = Javacord.getApi(this.token, true);
        this.api.connect(new FutureCallback<DiscordAPI>() {
            @Override
            public void onSuccess(final DiscordAPI api) {
                finishConnect();
            }

            @Override
            public void onFailure(Throwable t) {
                //login failed
                t.printStackTrace();
            }
        });
    }

    public void stop() {
        super.stop();
        this.api.disconnect();
    }

    public void finishConnect() {
        System.out.println("Discord connected, registering listeners...");
        this.listeners = new DiscordListener();
        this.api.registerListener(this.listeners);
        System.out.println("Listeners registered.");
        //Start a thread to check ranks
        this.server = this.api.getServerById(this.serverID);
        /*if (IssueTracker.DEBUG)
            for (Role r : this.server.getRoles())
                System.out.println(r.getName() + ' ' + r.getId());//*/
        this.mysql = new DiscordMySQL();
    }

    public Server getServer() {
        return this.server;
    }

    public DiscordAPI getApi() {
        return this.api;
    }

    public String getAuthMessage() {
        return this.authMessage;
    }
}