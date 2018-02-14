package gg.galaxygaming.janet;

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

            //Discord
            if (!containsKey("DISCORD_TOKEN"))
                setProperty("DISCORD_TOKEN", "token");
            if (!containsKey("DISCORD_SERVER"))
                setProperty("DISCORD_SERVER", "-1");
            if (!containsKey("DISCORD_VERIFIED"))
                setProperty("DISCORD_VERIFIED", "-1");
            if (!containsKey("DISCORD_STAFF"))
                setProperty("DISCORD_STAFF", "-1");
            if (!containsKey("DISCORD_SENIOR"))
                setProperty("DISCORD_SENIOR", "-1");
            if (!containsKey("DISCORD_DONOR"))
                setProperty("DISCORD_DONOR", "-1");
            if (!containsKey("DISCORD_SUPPORTER"))
                setProperty("DISCORD_SUPPORTER", "-1");
            if (!containsKey("DISCORD_USER_ROOMS"))
                setProperty("DISCORD_USER_ROOMS", "-1");
            if (!containsKey("DISCORD_AUTH_MESSAGE"))
                setProperty("DISCORD_AUTH_MESSAGE", "Go authenticate your account.");

            //Teamspeak
            if (!containsKey("TEAMSPEAK_VERIFIED"))
                setProperty("TEAMSPEAK_VERIFIED", "-1");
            if (!containsKey("TEAMSPEAK_USERNAME"))
                setProperty("TEAMSPEAK_USERNAME", "username");
            if (!containsKey("TEAMSPEAK_PASSWORD"))
                setProperty("TEAMSPEAK_PASSWORD", "password");
            if (!containsKey("TEAMSPEAK_PASSWORD"))
                setProperty("TEAMSPEAK_PASSWORD", "-1");
            if (!containsKey("TEAMSPEAK_JOIN"))
                setProperty("TEAMSPEAK_JOIN", "Welcome.");
            if (!containsKey("TEAMSPEAK_VERIFY"))
                setProperty("TEAMSPEAK_VERIFY", "Go verify.");
            if (!containsKey("TEAMSPEAK_ROOM_CREATOR"))
                setProperty("TEAMSPEAK_ROOM_CREATOR", "Join here to create a new room");
            if (!containsKey("TEAMSPEAK_DEFAULT"))
                setProperty("TEAMSPEAK_DEFAULT", "-1");
            if (!containsKey("TEAMSPEAK_USER_ROOMS"))
                setProperty("TEAMSPEAK_USER_ROOMS", "-1");
            if (!containsKey("TEAMSPEAK_CHANEL_ADMIN"))
                setProperty("TEAMSPEAK_CHANEL_ADMIN", "-1");
            if (!containsKey("TEAMSPEAK_SUPPORTER"))
                setProperty("TEAMSPEAK_SUPPORTER", "-1");

            //MySQL
            if (!containsKey("DB_HOST"))
                setProperty("DB_HOST", "127.0.0.1:3306");
            if (!containsKey("DB_NAME"))
                setProperty("DB_NAME", "database");
            if (!containsKey("DB_USER"))
                setProperty("DB_USER", "user");
            if (!containsKey("DB_PASSWORD"))
                setProperty("DB_PASSWORD", "password");
            if (!containsKey("GMOD_DB_USER"))
                setProperty("GMOD_DB_USER", "user");
            if (!containsKey("GMOD_DB_PASSWORD"))
                setProperty("GMOD_DB_PASSWORD", "password");
            if (!containsKey("GMOD_DB_NAME"))
                setProperty("GMOD_DB_NAME", "database");

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

    public long getLong(String key) {
        String value = getProperty(key);
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return 0;//TODO should there be a different default failed value?
        }
    }

    public long getLongOrDefault(String key, long defaultValue) {
        String value = getProperty(key);
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    //TODO add more default wrappers, such as boolean, double, maybe move the splitting by comma into an array wrapper
}