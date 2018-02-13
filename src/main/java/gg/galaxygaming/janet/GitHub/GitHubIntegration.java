package gg.galaxygaming.janet.GitHub;

import gg.galaxygaming.janet.base.AbstractIntegration;

public class GitHubIntegration extends AbstractIntegration {
    public boolean createIssue() {//Needs params
        return false;
    }

    //When an issue is closed send messages to proper locations, potentially some sort of listener
}