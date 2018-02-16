package gg.galaxygaming.janet.Discord;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.DiscordApiBuilder;
import de.btobastian.javacord.entities.Server;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.base.AbstractIntegration;

public class DiscordIntegration extends AbstractIntegration {
    private final long serverID, devChannel;
    private final String authMessage;
    private DiscordApi api;
    private DiscordListener listeners;
    private Server server;

    public DiscordIntegration() {
        super();
        Config config = Janet.getConfig();
        String token = config.getStringOrDefault("DISCORD_TOKEN", "token");
        this.serverID = config.getLongOrDefault("DISCORD_SERVER", -1);
        this.authMessage = config.getStringOrDefault("DISCORD_AUTH_MESSAGE", "Go authenticate your account.");
        this.devChannel = config.getLongOrDefault("DISCORD_DEV_CHANNEL", -1);
        if (token.equals("token") || this.serverID < 0 || this.devChannel < 0) {
            Janet.getLogger().error("Failed to load needed configs for Discord Integration");
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
        Janet.getLogger().info("Discord connected, registering listeners...");
        this.listeners = new DiscordListener();
        getApi().addMessageCreateListener(this.listeners);
        getApi().addServerMemberJoinListener(this.listeners);
        Janet.getLogger().info("Discord listeners registered.");
        getApi().getServerById(this.serverID).ifPresent(server -> this.server = server);
        this.mysql = new DiscordMySQL();
    }

    public long getDevChannel() {
        return this.devChannel;
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