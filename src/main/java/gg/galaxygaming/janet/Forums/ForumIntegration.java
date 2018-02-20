package gg.galaxygaming.janet.Forums;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import gg.galaxygaming.janet.Config;
import gg.galaxygaming.janet.Janet;
import gg.galaxygaming.janet.api.AbstractIntegration;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.Integration} for using the RestAPI on the forums.
 */
public class ForumIntegration extends AbstractIntegration {//TODO: JavaDoc this when writing autopromote
    private final String restURL, restAPIKey, acceptMessage;
    private final int janetID;
    private String auth = "";

    public ForumIntegration() {
        super();
        Config config = Janet.getConfig();
        this.restURL = config.getStringOrDefault("REST_URL", "rest_url");
        this.restAPIKey = config.getStringOrDefault("REST_API_KEY", "api_key");
        this.janetID = config.getIntegerOrDefault("JANET_FORUM_ID", 0);
        this.acceptMessage = config.getStringOrDefault("APP_ACCEPT_MESSAGE", "Accepted.");
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
    }

    public String getAcceptMessage() {
        return this.acceptMessage;
    }

    public int getJanetID() {
        return this.janetID;
    }

    //Loop over suggestion forums looking for new posts and things

    //Check reactions to a post

    //create github issue

    /**
     * Sends a POST request to the forums.
     * @param urlEnding The {@link String} to append to the REST API URL.
     * @param payload   The Json payload to send
     * @return The result.
     */
    @Nullable
    public JsonObject sendPOST(String urlEnding, JsonObject payload) {//TODO: Add nullable?
        JsonObject json = null;
        StringBuilder postData = new StringBuilder();
        payload.forEach((key, value) -> {
            if (postData.length() != 0)
                postData.append('&');
            try {
                postData.append(URLEncoder.encode(key, "UTF-8")).append('=').append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        try {
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");
            URL url = new URL(this.restURL + urlEnding);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", this.auth);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            con.setRequestProperty("Accept", "application/json,text/plain");
            con.setRequestMethod("POST");
            con.getOutputStream().write(postDataBytes);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                    response.append(inputLine);
                in.close();
                json = (JsonObject) Jsoner.deserialize(response.toString());
            }
            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Sends a GET request to the forums.
     * @param urlEnding The {@link String} to append to the REST API URL.
     * @return The result.
     */
    @Nullable
    public JsonObject sendGET(String urlEnding) {
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
}