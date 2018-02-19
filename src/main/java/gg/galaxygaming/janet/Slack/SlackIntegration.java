package gg.galaxygaming.janet.Slack;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import gg.galaxygaming.janet.CommandHandler.CommandSender;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.api.AbstractIntegration;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.Integration} to connect to the Slack server.
 */
public class SlackIntegration extends AbstractIntegration {
    //TODO convert more void methods to booleans to give error messages if things go wrong
    //TODO: Replace SlackUser with BaseSlackUser after implementing some required methods
    private final HashMap<String, SlackUser> userMap = new HashMap<>();
    private boolean isConnected;
    private final String token;
    private final String infoChannel;//TODO: maybe convert it to an array
    private WebSocket ws;
    private int id;

    public SlackIntegration() {
        Config config = Janet.getConfig();
        this.token = config.getStringOrDefault("SLACK_TOKEN", "token");
        this.infoChannel = config.getStringOrDefault("INFO_CHANNEL", "info_channel");
        if (this.token.equals("token") || this.infoChannel.equals("info_channel")) {
            Janet.getLogger().error("Failed to load needed configs for Slack Integration");
            return;
        }
        if (connect())
            Janet.getLogger().info("Connected to slack.");
        else
            Janet.getLogger().error("Failed to connect to slack.");
    }

    /**
     * Decodes the given slack message.
     * @param text The message to decode.
     * @return The clean readable message.
     */
    private String cleanChat(String text) {
        if (text == null || text.isEmpty())
            return "";
        //Users
        Matcher matcher = Pattern.compile("\\<@(.*?)\\>").matcher(text);
        while (matcher.find()) {
            String str = matcher.group(1);
            SlackUser user = getUserInfo(str);
            text = text.replace("<@" + str + '>', '@' + (user == null ? "null" : user.getName()));
        }
        //Channel
        matcher = Pattern.compile("\\<#(.*?\\|.*?)\\>").matcher(text);
        while (matcher.find()) {
            String str = matcher.group(1);
            text = text.replace("<#" + str + '>', '#' + str.split("\\|")[1]);
        }

        //URLS with http or https
        matcher = Pattern.compile("\\<(http[^\\|;]+)\\>").matcher(text);
        while (matcher.find()) {
            String str = matcher.group(1);
            text = text.replace('<' + str + '>', str);
        }

        //Date, email address, Remaining URLs
        matcher = Pattern.compile("\\<(.*?\\|.*?)\\>").matcher(text);
        while (matcher.find()) {
            String str = matcher.group(1);
            text = text.replace('<' + str + '>', str.split("\\|")[1]);
        }
        return text.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
    }

    public void stop() {
        super.stop();
        if (!isConnected)
            return;
        userMap.clear();
        sendMessage("Disconnected.", infoChannel);
        isConnected = false;
        if (ws != null)
            ws.disconnect();
    }

    /**
     * Connects to the Slack server.
     * @return True if this {@link gg.galaxygaming.janet.api.Integration} connected successfully, false otherwise.
     */
    private boolean connect() {
        if (isConnected)
            return false;
        try {
            URL url = new URL("https://slack.com/api/rtm.connect?token=" + token);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            StringBuilder response;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                response = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                    response.append(inputLine);
            }
            JsonObject json = Jsoner.deserialize(response.toString(), new JsonObject());
            String webSocketUrl = json.getString(Jsoner.mintJsonKey("url", null));
            if (webSocketUrl != null)
                openWebSocket(webSocketUrl);
        } catch (Exception ignored) {
            return false;
        }
        isConnected = true;
        sendMessage("Connected.", infoChannel);
        return true;
    }

    /**
     * Opens a {@link WebSocket} to the specified url.
     * @param url The url of the {@link WebSocket}.
     */
    private void openWebSocket(String url) {
        try {
            ws = new WebSocketFactory().createSocket(url).addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) {//TODO listen back for potential replies to stored message ids
                    //Janet.getLogger().debug("Received Slack message: " + message);
                    JsonObject json = Jsoner.deserialize(message, new JsonObject());
                    if (json.containsKey("type")) {
                        if (json.getString(Jsoner.mintJsonKey("type", null)).equals("message")) {
                            if (json.containsKey("bot_id"))
                                return;//TODO maybe figure out the userid of botid if there is any reason to support bot messages..
                            //TODO will probably require some sort of bot support depending on how the integration with gmod's issue reporting works
                            //TODO: Or create a class that is a superclass of SlackUser and also make a bot subytpe of it
                            SlackUser info = getUserInfo(json.getString(Jsoner.mintJsonKey("user", null)));
                            if (info == null)
                                return;
                            String channel = json.getString(Jsoner.mintJsonKey("channel", null));
                            sendSlackChat(info, cleanChat(json.getString(Jsoner.mintJsonKey("text", null))), channel);
                        }
                    }
                }
            }).connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the ID of the channel that this {@link gg.galaxygaming.janet.api.Integration} sends messages to.
     * @return The ID of the channel to send messages to.
     */
    public String getInfoChannel() {
        return this.infoChannel;
    }

    /**
     * Sends the given message to the given Slack channel.
     * @param message The message to send.
     * @param channel The ID of the Slack channel to send the message to.
     */
    public void sendMessage(String message, String channel) {
        if (message.endsWith("\n"))
            message = message.substring(0, message.length() - 1);
        JsonObject json = new JsonObject();
        json.put("id", this.id++);
        json.put("type", "message");
        json.put("channel", channel);
        json.put("text", message);
        ws.sendText(Jsoner.serialize(json));
    }

    /**
     * Retrieves a {@link SlackUser} by their Slack ID.
     * @param id The ID of the {@link SlackUser} to get.
     * @return The {@link SlackUser} with the given ID, or null if no {@link SlackUser} was found with the given ID.
     */
    private SlackUser getUserInfo(String id) {//TODO Try to make this more efficient, potentially improving the response reading
        SlackUser user = userMap.get(id);
        if (user != null)
            return user;
        try {
            URL url = new URL("https://slack.com/api/users.info?token=" + token + "&user=" + id);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();
            JsonObject jsonResponse = (JsonObject) Jsoner.deserialize(response.toString());
            if (!jsonResponse.getBooleanOrDefault(Jsoner.mintJsonKey("ok", false)))
                return null; //User does not exist or is deactivated
            JsonObject userInfo = (JsonObject) jsonResponse.get("user");
            if (userInfo.getBooleanOrDefault(Jsoner.mintJsonKey("deleted", false))) //This may not ever even be true, given account is deactivated
                return null;
            userMap.put(id, user = new SlackUser(userInfo));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    /**
     * Handles incoming chat from Slack.
     * @param info    The {@link SlackUser} who sent the message.
     * @param message The message the {@link SlackUser} sent.
     * @param channel The ID of the channel the message was sent in.
     */
    private void sendSlackChat(SlackUser info, String message, String channel) {
        if (info == null)
            return;
        if (info.getRank().isBanned()) {
            sendMessage("Error: You are restricted.", channel);
            return;
        }
        if (info.isBot()) //If bot don't send, we should process it but not ever as a command
            return;
        boolean isCommand = false;
        CommandSender sender = new CommandSender(info, channel);
        if (message.startsWith("!"))
            isCommand = Janet.getCommandHandler().handleCommand(message, sender);
        if (isCommand)
            return;
        if (!channel.startsWith("D")) {//If not pm, but should it also check private messages?
            //TODO: evaluate for enough information and then create a new issue on github
            if (this.infoChannel.equals(channel)) {
                Janet.getDiscord().getServer().getTextChannelById(Janet.getDiscord().getDevChannel()).ifPresent(c ->
                        c.sendMessage(info.getDisplayName() + ": " + message));
            }
        }//TODO: Maybe add some functionality if it is a pm
    }
}