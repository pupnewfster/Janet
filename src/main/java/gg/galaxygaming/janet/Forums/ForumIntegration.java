package gg.galaxygaming.janet.Forums;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.Utils;
import gg.galaxygaming.janet.api.AbstractIntegration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.Integration} for using the RestAPI on the forums.
 */
public class ForumIntegration extends AbstractIntegration {//TODO: JavaDoc this when writing autopromote
    private final String restURL;
    private final String restAPIKey;
    private final Map<Integer, List<Integer>> applicationForums;
    private final List<Integer> acceptedForums;
    private final List<Integer> deniedForums;
    private final int janetID;
    private Thread scan;
    private String auth = "";

    public ForumIntegration() {
        super();
        Config config = Janet.getConfig();
        this.restURL = config.getStringOrDefault("REST_URL", "rest_url");
        this.restAPIKey = config.getStringOrDefault("REST_API_KEY", "api_key");
        this.janetID = config.getIntegerOrDefault("JANET_FORUM_ID", 0);
        this.applicationForums = new HashMap<>();
        String[] application = config.getStringOrDefault("APPLICATION_FORUMS", "").split(",");
        for (String a : application) {
            if (Utils.legalInt(a))
                this.applicationForums.put(Integer.parseInt(a), new ArrayList<>());
            else
                Janet.getLogger().error("Invalid application forum: " + a);
        }
        this.acceptedForums = new ArrayList<>();
        String[] accepted = config.getStringOrDefault("ACCEPTED_FORUMS", "").split(",");
        for (String a : accepted) {
            if (Utils.legalInt(a))
                this.acceptedForums.add(Integer.parseInt(a));
            else
                Janet.getLogger().error("Invalid accepted forum: " + a);
        }
        this.deniedForums = new ArrayList<>();
        String[] denied = config.getStringOrDefault("DENIED_FORUMS", "").split(",");
        for (String d : denied) {
            if (Utils.legalInt(d))
                this.deniedForums.add(Integer.parseInt(d));
            else
                Janet.getLogger().error("Invalid denied forum: " + d);
        }
        if (this.restURL.equals("rest_url") || this.restAPIKey.equals("api_key")) {
            Janet.getLogger().error("Failed to load needed configs for Rest Integration");
            return;
        }
        try {
            this.auth = "Basic " + Base64.getEncoder().encodeToString((this.restAPIKey + ':').getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        this.mysql = new ForumMySQL();
        /*this.scan = new Thread(() -> {
            while (true) {
                scanApplications();
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        scan.start();*/
    }

    public void stop() {
        super.stop();
        scan.interrupt();
    }

    //Loop over suggestion forums looking for new posts and things

    //Check reactions to a post

    //create github issue

    private JsonObject sendPOST(String urlEnding, JsonObject payload) {
        try {
            URL url = new URL(this.restURL + urlEnding);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", this.auth);
            con.setRequestProperty("Content-Type", "application/json;");
            con.setRequestProperty("Accept", "application/json,text/plain");
            con.setRequestMethod("POST");
            try (OutputStream os = con.getOutputStream()) {
                os.write(Jsoner.serialize(payload).getBytes("UTF-8"));
            }
            /*try (InputStream is = con.getInputStream()) {
                //TODO include if we need to view error messages
            }*/
            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JsonObject sendGET(String urlEnding) {
        JsonObject json = null;
        try {
            URL url = new URL(this.restURL + urlEnding);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", this.auth);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json,text/plain");//Does having this break things?
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                    content.append(inputLine);
                json = Jsoner.deserialize(content.toString(), new JsonObject());
            }
            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    private void scanApplications() {
        Janet.getLogger().info("Application scan started.");
        scanNewApplications();
        checkApplicationStatus();
        Janet.getLogger().info("Application scan finished.");
    }

    //TODO: Maybe make this check in a separate thread
    private void scanNewApplications() {
        Set<Map.Entry<Integer, List<Integer>>> appForums = this.applicationForums.entrySet();
        for (Map.Entry<Integer, List<Integer>> forumInfo : appForums) {
            int fid = forumInfo.getKey();
            List<Integer> topics = forumInfo.getValue();
            //GET request to get topics
            JsonObject forum = sendGET("/forums/topics?forums=" + Integer.toString(fid));
            JsonArray rTopics = (JsonArray) forum.get("results");
            for (Object t : rTopics) {
                JsonObject topic = (JsonObject) t;
                Integer topicID = topic.getIntegerOrDefault(Jsoner.mintJsonKey("id", null));
                if (topicID != null && !topics.contains(topicID)) {
                    topics.add(topicID);
                    Janet.getLogger().debug("Topic added: " + topicID);
                }
            }
            //TODO if there are multiple pages scan next pages also
            //totalPages
            //page
        }
    }

    private void checkApplicationStatus() {
        Set<Map.Entry<Integer, List<Integer>>> appForums = this.applicationForums.entrySet();
        for (Map.Entry<Integer, List<Integer>> forumInfo : appForums) {
            int fid = forumInfo.getKey();
            List<Integer> topics = forumInfo.getValue();
            List<Integer> remTopics = new ArrayList<>();
            for (Integer topicID : topics) {
                JsonObject topic = sendGET("/forums/topics/" + topicID);
                if (topic.containsKey("errorCode")) { //Topic was deleted
                    Janet.getLogger().debug("Error Code:" + topic.getStringOrDefault(Jsoner.mintJsonKey("errorMessage", "UNKNOWN")));
                    remTopics.add(topicID);
                    continue;
                }
                JsonObject forum = (JsonObject) topic.get("forum");
                int forumID = forum.getInteger(Jsoner.mintJsonKey("id", null));
                if (fid != forumID) {
                    remTopics.add(topicID); //Can be here because it was moved no matter where it is now
                    if (this.acceptedForums.contains(forumID)) {//Accepted
                        Janet.getLogger().debug("Topic " + topicID + " ACCEPTED.");
                        JsonObject post = (JsonObject) topic.get("firstPost");//TODO: maybe move this up if the post info is needed for other things
                        JsonObject member = (JsonObject) post.get("author");
                        String email = member.getString(Jsoner.mintJsonKey("email", null));
                        String response = sendInvite(email);
                        if (response != null) {
                            JsonObject json = new JsonObject();
                            json.put("topic", topicID);
                            json.put("author", this.janetID);
                            json.put("post", "<p>" + response + "</p>");
                            Janet.getLogger().debug("Post to forums.");
                            sendPOST("/forums/posts", json);
                        }
                        //Log some sort of error message
                    } else if (this.deniedForums.contains(forumID)) {//Denied
                        //No denied message, may want to send a debug message
                        Janet.getLogger().info("Topic " + topicID + " DENIED.");
                    } else {
                        Janet.getLogger().info("[DEBUG] Topic " + topicID + " moved to " + forumID + '.');
                    }
                }
            }
            topics.removeAll(remTopics); //Removes all topics no longer in this forum
        }
    }

    private String sendInvite(String email) {
        //TODO remove this method and change what checkApplicationStatus does
        return null;
    }
}