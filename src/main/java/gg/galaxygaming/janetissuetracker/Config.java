package gg.galaxygaming.janetissuetracker;

import java.io.*;
import java.util.Properties;

/**
 * A basic wrapper for Properties.
 * This allows converting values to types such as Booleans, without having to repeat logic.
 */
public class Config extends Properties {
    public Config() {
        File config = new File("config.cfg");
        if (!config.exists()) {
            try {
                if (!config.createNewFile()) {
                    System.out.println("[WARNING] Failed to create config file");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileInputStream input = new FileInputStream(config)) {//Load current settings
            load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        addMissingValues(config);
    }

    private void addMissingValues(File config) {//TODO: should we store the default values in a map so that we can also get them
        try (OutputStream output = new FileOutputStream(config)) {
            if (!containsKey("SLACK_TOKEN"))
                setProperty("SLACK_TOKEN", "token");
            if (!containsKey("USER_SLACK_TOKEN"))
                setProperty("USER_SLACK_TOKEN", "token");
            if (!containsKey("INFO_CHANNEL"))
                setProperty("INFO_CHANNEL", "info_channel");

            //REST Config Options
            if (!containsKey("REST_URL"))
                setProperty("REST_URL", "rest_url");
            if (!containsKey("REST_API_KEY"))
                setProperty("REST_API_KEY", "api_key");
            if (!containsKey("JANET_FORUM_ID"))
                setProperty("JANET_FORUM_ID", "0");
            if (!containsKey("APPLICATION_FORUMS"))
                setProperty("APPLICATION_FORUMS", "");
            if (!containsKey("ACCEPTED_FORUMS"))
                setProperty("ACCEPTED_FORUMS", "");
            if (!containsKey("DENIED_FORUMS"))
                setProperty("DENIED_FORUMS", "");

            if (!containsKey("INVALID_EMAIL"))
                setProperty("INVALID_EMAIL", "Invalid email, contact Senior Staff.");
            if (!containsKey("INVITE_SUCCESS"))
                setProperty("INVITE_SUCCESS", "Invited to slack.");

            store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public String getString(String key) {
        return getProperty(key);
    }

    public String getStringOrDefault(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }

    public int getInteger(String key) {
        String value = getProperty(key);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return 0;//TODO should there be a different default failed value?
        }
    }

    public int getIntegerOrDefault(String key, int defaultValue) {
        String value = getProperty(key);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    //TODO add more default wrappers, such as boolean, double, maybe move the splitting by comma into an array wrapper
}