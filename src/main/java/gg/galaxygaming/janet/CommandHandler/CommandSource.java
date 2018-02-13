package gg.galaxygaming.janet.CommandHandler;

public enum CommandSource {
    //TODO: Add Discord and TeamSpeak integration at some point?? Afterwards add them as CommandSources
    Slack("Slack"),
    TeamSpeak("TeamSpeak"),
    Discord("Discord"),
    Console("Console");//TODO: Implement some commands for the console in the future

    private final String name;

    CommandSource(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}