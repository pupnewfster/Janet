package gg.galaxygaming.janetissuetracker.Slack;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import gg.galaxygaming.janetissuetracker.Config;
import gg.galaxygaming.janetissuetracker.IssueTracker;
import gg.galaxygaming.janetissuetracker.Utils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class JanetSlack {//TODO: add in proper javadoc explanations for methods
    //TODO convert more void methods to booleans to give error messages if things go wrong
    private final HashMap<String, SlackUser> userMap = new HashMap<>();
    private final HashMap<Integer, ArrayList<String>> helpLists = new HashMap<>();
    private boolean isConnected;
    private String token;
    private String infoChannel;//TODO: maybe convert it to an array
    private WebSocket ws;

    //TODO: use RTM member_joined_channel to tell when they joined the server
    //TODO: Make sure that user mentions convert to proper name and back

    public JanetSlack(Config config) {
        token = config.getStringOrDefault("SLACK_TOKEN", "token");
        infoChannel = config.getStringOrDefault("INFO_CHANNEL", "info_channel");
        if (token.equals("token") || infoChannel.equals("info_channel")) {
            System.out.println("[ERROR] Failed to load needed configs for Slack Integration");
            return;
        }
        if (!setHelp())
            System.out.println("[ERROR] Failed to set help messages.");
        if (connect())
            System.out.println("Connected to slack.");
        else
            System.out.println("[ERROR] Failed to connect to slack.");
    }

    public void disconnect() {
        if (!isConnected)
            return;
        userMap.clear();
        helpLists.clear();
        sendMessage("Disconnected.", infoChannel);
        isConnected = false;
        if (ws != null)
            ws.disconnect();
    }

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

    private void openWebSocket(String url) {
        try {
            ws = new WebSocketFactory().createSocket(url).addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) {//TODO listen back for potential replies to stored message ids
                    if (IssueTracker.DEBUG)
                        System.out.println("[DEBUG] Received Slack message: " + message);
                    JsonObject json = Jsoner.deserialize(message, new JsonObject());
                    if (json.containsKey("type")) {
                        if (json.getString(Jsoner.mintJsonKey("type", null)).equals("message")) {//TODO see if there is an id field and how it acts
                            if (json.containsKey("bot_id"))
                                return;//TODO maybe figure out the userid of botid if there is any reason to support bot messages..
                            //TODO will probably require some sort of bot support depending on how the integration with gmod's issue reporting works
                            //TODO: Or create a class that is a superclass of SlackUser and also make a bot subytpe of it
                            SlackUser info = getUserInfo(json.getString(Jsoner.mintJsonKey("user", null)));
                            if (info == null)
                                return;
                            String text = json.getString(Jsoner.mintJsonKey("text", null));
                            while (text.contains("<") && text.contains(">")) {
                                //TODO: test with multiple @mentions, probably does not work and needs a rewrite
                                //TODO: In rewrite also include the ability to support other formatted things such as channels
                                String[] split = text.split("<@");
                                String[] sSplit = split[1].split(">:");
                                SlackUser user = getUserInfo(sSplit[0]);
                                text = split[0] + '@' + (user == null ? "null" : user.getName()) + ':' + sSplit[1];
                            }
                            String channel = json.getString(Jsoner.mintJsonKey("channel", null));
                            sendSlackChat(info, text, channel);
                            //if (channel.startsWith("D")) //Direct Message
                            //else if (channel.startsWith("C") || channel.startsWith("G")) //Channel or Group
                        }
                    }
                }
            }).connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: Probably remove, unless some use of user info can be found... such as for better indexing of id
    /*public void sendMessage(String message, String channel, SlackUser u) {
        sendMessage(message, channel);
    }*/

    public void sendMessage(String message, String channel) {
        if (message.endsWith("\n"))
            message = message.substring(0, message.length() - 1);
        JsonObject json = new JsonObject();
        int id = 1;//TODO: increment id, and keep track of ones that a response is wanted from
        json.put("id", id);
        json.put("type", "message");
        json.put("channel", channel);
        json.put("text", message);
        ws.sendText(Jsoner.serialize(json));//TODO make sure this works
    }

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

    public InviteResponse inviteUser(String email) {//TODO make sure that they are not restricted/ultrarestricted
        try {//TODO check response and make sure there are no errors
            URL url = new URL("https://slack.com/api/users.admin.invite?token=" + token + "&email=" + email);
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
                return InviteResponse.fromString(jsonResponse.getStringOrDefault(Jsoner.mintJsonKey("error", "other")));
        } catch (Exception ignored) {
            return InviteResponse.OTHER;
        }
        return InviteResponse.SUCCESS;
    }

    private String getLine(int page, int time, ArrayList<String> helpList) {
        if (time == 10)
            return null;
        page *= 10;
        if (helpList.size() < time + page + 1)
            return null;
        return helpList.get(page + time);
    }

    @SuppressWarnings("unchecked")
    private boolean setHelp() {//TODO: potentially create commands and if so set them here in the help instead of including them all below in sendSlackChat
        //TODO: try to come up with a way to not require cloning the help lists for higher ranks.
        //TODO This could be done with them all being individual commands and then checking to see if rank is valid for showing, how to decided on page count
        ArrayList<String> temp = new ArrayList<>();
        temp.add("!help <page> ~ View the help messages on <page>.");
        temp.add("!rank ~ Shows you what rank you have.");
        helpLists.put(0, (ArrayList<String>) temp.clone());//Member
        helpLists.put(1, (ArrayList<String>) temp.clone());//Admin
        helpLists.put(2, (ArrayList<String>) temp.clone());//Owner
        helpLists.put(3, (ArrayList<String>) temp.clone());//Primary owner
        temp.clear();
        return true;
    }

    private void sendSlackChat(SlackUser info, String message, String channel) {//TODO: Maybe add some functionality if it is a pm
        //TODO include the channel it is sent from
        if (info == null)
            return;
        if (info.isRestricted()) {
            sendMessage("Error: You are " + (info.isUltraRestricted() ? "ultra " : "") + "restricted.", channel);
            return;
        }
        if (info.isBot()) //If bot don't send to game
            return;
        final String name = info.getName();
        if (message.startsWith("!")) {
            boolean isCommand = true;
            String m = "";
            if (message.startsWith("!help")) {//TODO: Reformat help to have better line endings/stick out more
                int page = 0;//TODO see if this can be rewritten to be more efficient instead of being the algorithm that has been slowly upgraded over the years
                if (message.split(" ").length > 1 && !Utils.legalInt(message.split(" ")[1])) {
                    sendMessage("Error: You must enter a valid help page.", channel);
                    return;
                }
                if (message.split(" ").length > 1)
                    page = Integer.parseInt(message.split(" ")[1]);
                if (message.split(" ").length == 1 || page <= 0)
                    page = 1;
                int time = 0;
                int rounder = 0;
                ArrayList<String> helpList = helpLists.get(info.getRank());
                if (helpList.size() % 10 != 0)
                    rounder = 1;
                int totalpages = helpList.size() / 10 + rounder;
                if (page > totalpages) {
                    sendMessage("Error: Input a number from 1 to " + Integer.toString(totalpages), channel);
                    return;
                }
                m += " ---- Help -- Page " + Integer.toString(page) + '/' + Integer.toString(totalpages) + " ---- \n";
                page = page - 1;
                String msg = getLine(page, time, helpList);
                StringBuilder mBuilder = new StringBuilder(m);
                while (msg != null) {
                    mBuilder.append(msg).append('\n');
                    time++;
                    msg = getLine(page, time, helpList);
                }
                m = mBuilder.toString();
                if (page + 1 < totalpages)
                    m += "Type !help " + Integer.toString(page + 2) + " to read the next page.\n";
            } else if (message.startsWith("!rank"))
                m += info.getRankName() + '\n';
            else
                isCommand = false;
            if (isCommand) {
                sendMessage(m, channel);
                return;
            }
        }
        if (!channel.startsWith("D")) {//If not pm, but should it also check private messages?
            //TODO: evaluate for enough information and then create a new issue on github
        }
    }
}