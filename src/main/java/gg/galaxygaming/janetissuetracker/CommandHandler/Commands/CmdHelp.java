package gg.galaxygaming.janetissuetracker.CommandHandler.Commands;

import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSender;
import gg.galaxygaming.janetissuetracker.CommandHandler.CommandSource;
import gg.galaxygaming.janetissuetracker.CommandHandler.RankTree;
import gg.galaxygaming.janetissuetracker.IssueTracker;
import gg.galaxygaming.janetissuetracker.Utils;

import java.util.ArrayList;
import java.util.List;

public class CmdHelp implements Cmd {
    @Override
    public boolean performCommand(String[] args, CommandSender sender) {
        if (args.length > 0 && !Utils.legalInt(args[0])) {
            sender.sendMessage("Error: You must enter a valid help page.");
            return true;
        }
        int page = 0;
        if (args.length > 0)
            page = Integer.parseInt(args[0]);
        if (args.length == 0 || page <= 0)
            page = 1;
        int rounder = 0;
        ArrayList<String> helpList = IssueTracker.getCommandHandler().getHelpList(sender);
        if (helpList.size() % 10 != 0)
            rounder = 1;
        int totalPages = helpList.size() / 10 + rounder;
        if (page > totalPages) {
            sender.sendMessage("Error: Input a number from 1 to " + totalPages);
            return true;
        }
        int time = 0;
        StringBuilder m = new StringBuilder(" ---- Help -- Page " + page + '/' + totalPages + " ---- \n");
        page = page - 1;
        String msg;
        while ((msg = getLine(page, time++, helpList)) != null)
            m.append(msg).append('\n');
        if (page + 1 < totalPages)
            m.append("Type !help ").append(page + 2).append(" to read the next page.\n");
        sender.sendMessage(m.toString());
        return true;
    }


    private String getLine(int page, int time, ArrayList<String> helpList) {
        //page *= 10;
        return helpList.size() < time + (page *= 10) + 1 || time == 10 ? null : helpList.get(page + time);
    }

    @Override
    public String helpDoc() {
        return "View the help messages on <page>.";
    }

    @Override
    public String getUsage() {
        return "!help <page>";
    }

    @Override
    public String getName() {
        return "Help";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public List<CommandSource> supportedSources() {
        return null;
    }

    @Override
    public RankTree getRequiredRank() {
        return RankTree.MEMBER;
    }
}