package gg.galaxygaming.janet.command_handler;

/**
 * An enum for which platform {@link gg.galaxygaming.janet.api.Cmd}s can come from.
 */
public enum CommandSource {
    /**
     * The {@link CommandSource} of {@link gg.galaxygaming.janet.api.Cmd} is Slack
     */
    Slack("Slack"),
    /**
     * The {@link CommandSource} of {@link gg.galaxygaming.janet.api.Cmd} is TeamSpeak
     */
    TeamSpeak("TeamSpeak"),
    /**
     * The {@link CommandSource} of {@link gg.galaxygaming.janet.api.Cmd} is Discord
     */
    Discord("Discord"),
    /**
     * The {@link CommandSource} of {@link gg.galaxygaming.janet.api.Cmd} is the Console
     */
    Console("Console");//TODO: Implement some commands for the console in the future

    private final String name;

    CommandSource(String name) {
        this.name = name;
    }

    /**
     * Gets the name of this {@link CommandSource}.
     * @return The name of this {@link CommandSource}.
     */
    public String getName() {
        return this.name;
    }
}