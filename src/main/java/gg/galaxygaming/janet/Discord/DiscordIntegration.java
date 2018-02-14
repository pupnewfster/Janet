package gg.galaxygaming.janet.Discord;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.DiscordApiBuilder;
import de.btobastian.javacord.entities.Server;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.base.AbstractIntegration;

public class DiscordIntegration extends AbstractIntegration {
    private final String authMessage;
    private DiscordApi api;
    private DiscordListener listeners;
    private long serverID;
    private Server server;

    public DiscordIntegration() {
        Config config = Janet.getConfig();
        String token = config.getStringOrDefault("DISCORD_TOKEN", "token");
        this.serverID = config.getLongOrDefault("DISCORD_SERVER", -1);
        this.authMessage = config.getStringOrDefault("DISCORD_AUTH_MESSAGE", "Go authenticate your account.");
        if (token.equals("token") || this.serverID < 0) {
            System.out.println("[ERROR] Failed to load needed configs for Discord Integration");
            return;
        }
        new DiscordApiBuilder().setToken(token).login().thenAccept(api -> {
            this.api = api;
            //Login successful
            finishConnect();
        });
    }

    public void stop() {
        super.stop();
        getApi().disconnect();
    }

    public void finishConnect() {
        System.out.println("Discord connected, registering listeners...");
        this.listeners = new DiscordListener();
        getApi().addMessageCreateListener(this.listeners);
        getApi().addServerMemberJoinListener(this.listeners);
        System.out.println("Listeners registered.");
        getApi().getServerById(this.serverID).ifPresent(server -> this.server = server);
        this.mysql = new DiscordMySQL();
    }

    public Server getServer() {
        return this.server;
    }

    public DiscordApi getApi() {
        return this.api;
    }

    public String getAuthMessage() {
        return this.authMessage;
    }
}