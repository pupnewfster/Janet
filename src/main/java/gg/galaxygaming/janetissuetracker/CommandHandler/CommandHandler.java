package gg.galaxygaming.janetissuetracker.CommandHandler;

import gg.galaxygaming.janetissuetracker.CommandHandler.Commands.Cmd;

import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandHandler {
    private final ArrayList<Cmd> cmds = new ArrayList<>();

    public CommandHandler(String path) {
        Reflections reflections = new Reflections(path);
        Set<Class<? extends Cmd>> subTypes = reflections.getSubTypesOf(Cmd.class);
        subTypes.forEach(c -> loadCommand(c.getSimpleName(), path + '.'));
    }

    private void loadCommand(String name, String pkg) {
        try {
            Cmd command = (Cmd) Cmd.class.getClassLoader().loadClass(pkg + name).newInstance();
            if (command != null)
                this.cmds.add(command);
        } catch (Exception ignored) {
        }
    }


    public boolean handleCommand(String message, CommandSender sender) {//TODO: Call this
        if (sender == null)
            return false;
        if (message.startsWith("!"))
            message = message.replaceFirst("!", "");
        String command = message.split(" ")[0];
        String arguments = message.replaceFirst(command, "").trim();
        String[] args = arguments.equals("") ? new String[0] : arguments.split(" ");
        CommandSource source = sender.getSource();
        for (Cmd cmd : this.cmds) {
            if (!sender.getRank().hasRank(cmd.getRequiredRank())) {
                sender.sendMessage("Error: You do not have permission to use this command.");
                return true;
            }
            if (cmd.getName().equalsIgnoreCase(command) || cmd.getAliases() != null && cmd.getAliases().contains(command.toLowerCase())) {
                List<CommandSource> sources = cmd.supportedSources();
                if (sources != null && !sources.contains(source)) {
                    StringBuilder validSources = new StringBuilder();
                    for (int i = 0; i < sources.size(); i++) {
                        if (!validSources.toString().equals("")) {
                            validSources.append(i == 2 && i == sources.size() ? " " : ", ");
                            if (i == sources.size())
                                validSources.append("or ");
                        }
                        validSources.append(sources.get(i));
                    }
                    sender.sendMessage("Error: This command must be used through " + validSources);
                    return true;
                }
                return cmd.performCommand(args, sender);
            }
        }
        return false;
    }

    public ArrayList<String> getHelpList(CommandSender sender) {
        ArrayList<String> help = new ArrayList<>();
        this.cmds.stream().filter(cmd -> cmd.getName() != null && cmd.getUsage() != null && cmd.helpDoc() != null &&
                sender.getRank().hasRank(cmd.getRequiredRank())).forEach(cmd -> {
            List<CommandSource> sources = cmd.supportedSources();
            if (sources == null || sources.contains(sender.getSource()))
                help.add(cmd.getUsage() + " ~ " + cmd.helpDoc());
        });
        return help;
    }
}