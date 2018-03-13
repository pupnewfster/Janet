package gg.galaxygaming.janet.GitHub;

import com.neovisionaries.ws.client.WebSocket;
import gg.galaxygaming.janet.api.AbstractIntegration;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.Integration} to handle interaction
 * with the GitHub IssueTracker.
 */
public class GitHubIntegration extends AbstractIntegration {
    private WebSocket ws;//https://dzone.com/articles/building-a-realtime-github-integration-using-java

    public GitHubIntegration() {//TODO Use GraphQL to utilize GitHub API v4

    }
    //When an issue is closed send messages to proper locations, potentially some sort of listener
}