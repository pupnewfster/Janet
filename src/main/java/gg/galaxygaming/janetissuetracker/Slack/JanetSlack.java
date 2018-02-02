package gg.galaxygaming.janetissuetracker.Slack;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import gg.galaxygaming.janetissuetracker.Utils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

//Partial strip down of core from JanetSlack for Necessities. This will end up receiving a rather large rewrite, but will be useful for reference during the rewrite.
public class JanetSlack {//TODO: add in proper javadoc explanations for methods
    //TODO: Potentially rewrite how users are stored or not keep track of them because it probably does not matter
    //TODO: See if there is a newer way for integration that doesn't require sending posts that may be faster somehow. potentially a full slack app instead of webapp
    //TODO: Update simple json usage to make sure the old code is working properly with the new version of json-simple after its major refactor
    //TODO convert more void methods to booleans to give error messages if things go wrong
    private final HashMap<String, SlackUser> userMap = new HashMap<>();
    private final HashMap<Integer, ArrayList<String>> helpLists = new HashMap<>();
    private boolean isConnected;
    private String token;
    private URL hookURL;
    private WebSocket ws;

    public void init() {
        Properties config = new Properties();//TODO load properties
        token = config.containsKey("Necessities.SlackToken") ? config.getProperty("Necessities.SlackToken") : "token";
        String hook = config.containsKey("Necessities.WebHook") ? config.getProperty("Necessities.WebHook") : "webHook";
        if (token.equals("token") || hook.equals("webHook")) {
            //Failed print message
            return;
        }
        try {
            hookURL = new URL(hook);
        } catch (Exception e) {
            //Failed print message
            return;
        }
        if (!setHelp()) {
            //Failed to set help messages
        }
        if (connect()) {
            //Connected succesfully
        } else {
            //Failed print message
        }
    }

    public void disconnect() {
        if (!isConnected)
            return;
        userMap.clear();
        helpLists.clear();
        sendMessage("Disconnected.");
        sendPost("https://slack.com/api/users.setPresence?token=" + token + "&presence=away");
        isConnected = false;
        if (ws != null)
            ws.disconnect();
    }

    /**
     * Sends a message to slack.
     * @param message The message to send.
     * @param isPM    True if the message was a private message, false otherwise.
     * @param u       The SlackUser who sent the pm if there was one.
     */
    public void sendMessage(String message, boolean isPM, SlackUser u) {
        if (isPM)
            u.sendPrivateMessage(message);
        else
            sendMessage(message);
    }

    /**
     * Sends a message to slack.
     * @param message The message to send.
     */
    //sendPost("https://slack.com/api/chat.postMessage?token=" + token + "&channel=%23" + channel + "&text=" + ChatColor.stripColor(message.replaceAll(" ", "%20")) + "&as_user=true&pretty=1");
    public void sendMessage(String message) {
        if (message.endsWith("\n"))
            message = message.substring(0, message.length() - 1);
        JsonObject json = new JsonObject();
        json.put("text", message);
        try {
            HttpsURLConnection con = (HttpsURLConnection) hookURL.openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json;");
            con.setRequestProperty("Accept", "application/json,text/plain");
            con.setRequestMethod("POST");
            OutputStream os = con.getOutputStream();
            os.write(Jsoner.serialize(json).getBytes("UTF-8"));
            os.close();
            InputStream is = con.getInputStream();
            is.close();
            con.disconnect();
        } catch (Exception ignored) {
        }
    }

    private SlackUser getUserInfo(String id) {//TODO remove if not needed or rewrite (if possible) to get more efficiently
        if (userMap.containsKey(id))
            return userMap.get(id);
        //Almost never should get past this point as it maps the users when it connects unless a new user gets invited
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
            userMap.put(id, new SlackUser((JsonObject) Jsoner.deserialize(response.toString())));
        } catch (Exception ignored) {
        }
        return userMap.get(id);
    }

    private void sendPost(String url) {
        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            InputStream is = con.getInputStream();
            is.close();
            con.disconnect();
        } catch (Exception ignored) {
        }
    }

    private boolean connect() {
        if (isConnected)
            return false;
        try {
            URL url = new URL("https://slack.com/api/rtm.connect?token=" + token);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();
            JsonObject json = Jsoner.deserialize(response.toString(), new JsonObject());
            String webSocketUrl = json.getString(Jsoner.mintJsonKey("url", null));
            if (webSocketUrl != null)
                openWebSocket(webSocketUrl);
        } catch (Exception ignored) {
            return false;
        }
        setUsers();
        setUserChannels();
        sendPost("https://slack.com/api/users.setPresence?token=" + this.token + "&presence=auto");
        isConnected = true;
        sendMessage("Connected.");
        return true;
    }

    private void setUsers() {
        try {
            URL url = new URL("https://slack.com/api/users.list?token=" + token + "&presence=true");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();
            JsonObject json = Jsoner.deserialize(response.toString(), new JsonObject());
            //Map users
            JsonArray users = (JsonArray) json.get("members");
            for (Object u : users) {
                JsonObject user = (JsonObject) u;
                if (user.getBoolean(Jsoner.mintJsonKey("deleted", null)))
                    continue;
                String id = user.getString(Jsoner.mintJsonKey("id", null));
                if (!userMap.containsKey(id))
                    userMap.put(id, new SlackUser(user));
            }
        } catch (Exception ignored) {
        }
    }

    private void openWebSocket(String url) {
        try {
            ws = new WebSocketFactory().createSocket(url).addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) {
                    JsonObject json = Jsoner.deserialize(message, new JsonObject());
                    if (json.containsKey("type")) {
                        if (json.getString(Jsoner.mintJsonKey("type", null)).equals("message")) {
                            //TODO: Figure out if there is a way to get the user id of a bot instead of just using janet's
                            SlackUser info = json.containsKey("bot_id") ? getUserInfo("U2Y19AVNJ") : getUserInfo(json.getString(Jsoner.mintJsonKey("user", null)));
                            String text = json.getString(Jsoner.mintJsonKey("text", null));
                            while (text.contains("<") && text.contains(">"))
                                text = text.split("<@")[0] + '@' + getUserInfo(text.split("<@")[1].split(">:")[0]).getName() + ':' + text.split("<@")[1].split(">:")[1];
                            String channel = json.getString(Jsoner.mintJsonKey("channel", null));
                            if (channel.startsWith("D")) //Direct Message
                                sendSlackChat(info, text, true);
                            else if (channel.startsWith("C") || channel.startsWith("G")) //Channel or Group
                                sendSlackChat(info, text, false);
                        }
                    }
                }
            }).connect();
        } catch (Exception ignored) {
        }
    }

    private void setUserChannels() {
        try {
            URL url = new URL("https://slack.com/api/im.list?token=" + token);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();
            JsonObject json = Jsoner.deserialize(response.toString(), new JsonObject());
            //Map user channels
            for (Object i : (JsonArray) json.get("ims")) {
                JsonObject im = (JsonObject) i;
                String userID = im.getString(Jsoner.mintJsonKey("user", null));
                if (userMap.containsKey(userID))
                    userMap.get(userID).setChannel(im.getString(Jsoner.mintJsonKey("id", null)));
            }
        } catch (Exception ignored) {
        }
    }

    private String getLine(int page, int time, ArrayList<String> helpList) {
        page *= 10;
        if (helpList.size() < time + page + 1 || time == 10)
            return null;
        return helpList.get(page + time);
    }

    @SuppressWarnings("unchecked")
    private boolean setHelp() {//TODO: potentially create commands and if so set them here in the help
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

    private void sendSlackChat(SlackUser info, String message, boolean isPM) {//TODO: Maybe add some functionality if it is a pm
        if (!info.isMember()) {
            sendMessage("Error: You are restricted or ultra restricted", isPM, info);
            return;
        }
        if (info.isBot) //If bot don't send to game
            return;
        final String name = info.getName();
        if (message.startsWith("!")) {
            String m = "";
            if (message.startsWith("!help")) {//TODO: Reformat help to have better line endings/stick out more
                int page = 0;
                if (message.split(" ").length > 1 && !Utils.legalInt(message.split(" ")[1])) {
                    sendMessage("Error: You must enter a valid help page.", isPM, info);
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
                    sendMessage("Error: Input a number from 1 to " + Integer.toString(totalpages), isPM, info);
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
            } else if (message.startsWith("!rank")) {
                m += info.getRankName() + '\n';
            } else if (!isPM) {
                //TODO: evaluate for enough information and then create a new issue on github
                return;
            }
            sendMessage(m, isPM, info);
        } else if (!isPM) {
            //TODO: evaluate for enough information and then create a new issue on github
        }
    }

    @SuppressWarnings("unused")
    class SlackUser {
        private boolean justLoaded = true, viewingChat, isBot;
        private final String id;
        private final String name;
        private String latest;
        private String channel;
        private int rank;

        SlackUser(JsonObject json) {
            this.id = json.getString(Jsoner.mintJsonKey("id", null));
            this.name = json.getString(Jsoner.mintJsonKey("name", null));
            if (json.getBoolean(Jsoner.mintJsonKey("is_bot", null))) {
                this.isBot = true;
                this.rank = 2;
            } else if (json.getBoolean(Jsoner.mintJsonKey("is_primary_owner", null)))
                this.rank = 3;
            else if (json.getBoolean(Jsoner.mintJsonKey("is_owner", null)))
                this.rank = 2;
            else if (json.getBoolean(Jsoner.mintJsonKey("is_admin", null)))
                this.rank = 1;
            else if (json.getBoolean(Jsoner.mintJsonKey("is_ultra_restricted", null)))
                this.rank = -2;
            else if (json.getBoolean(Jsoner.mintJsonKey("is_restricted", null)))
                this.rank = -1;
            //else leave it at 0 for member
        }

        String getName() {
            return this.name;
        }

        String getID() {
            return this.id;
        }

        int getRank() {
            return this.rank;
        }

        boolean isBot() {
            return this.isBot;
        }

        boolean isUltraRestricted() {
            return this.rank >= -2;
        }

        boolean isRestricted() {
            return this.rank >= -1;
        }

        boolean isMember() {
            return this.rank >= 0;
        }

        boolean isAdmin() {
            return this.rank >= 1;
        }

        boolean isOwner() {
            return this.rank >= 2;
        }

        boolean isPrimaryOwner() {
            return this.rank >= 3;
        }

        String getRankName() {
            if (isPrimaryOwner())
                return "Primary Owner";
            else if (isOwner())
                return "Owner";
            else if (isAdmin())
                return "Admin";
            else if (isMember())
                return "Member";
            else if (isRestricted())
                return "Restricted";
            else if (isUltraRestricted())
                return "Ultra Restricted";
            return "Error";
        }

        boolean viewingChat() {
            return this.viewingChat;
        }

        void toggleViewingChat() {
            this.viewingChat = !this.viewingChat;
        }

        void setJustLoaded(boolean loaded) {
            this.justLoaded = loaded;
        }

        boolean getJustLoaded() {
            return this.justLoaded;
        }

        String getLatest() {
            return this.latest;
        }

        void setLatest(String latest) {
            this.latest = latest;
        }

        String getChannel() {
            return this.channel;
        }

        void setChannel(String channel) {
            this.channel = channel;
        }

        void sendPrivateMessage(String message) {
            if (message.endsWith("\n"))
                message = message.substring(0, message.length() - 1);
            JsonObject json = new JsonObject();
            json.put("text", message);
            json.put("channel", this.channel);
            try {
                HttpsURLConnection con = (HttpsURLConnection) hookURL.openConnection();
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json;");
                con.setRequestProperty("Accept", "application/json,text/plain");
                con.setRequestMethod("POST");
                OutputStream os = con.getOutputStream();
                os.write(Jsoner.serialize(json).getBytes("UTF-8"));
                os.close();
                InputStream is = con.getInputStream();
                is.close();
                con.disconnect();
            } catch (Exception ignored) {
            }
        }
    }
}