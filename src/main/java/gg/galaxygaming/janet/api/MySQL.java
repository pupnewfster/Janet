package gg.galaxygaming.janet.api;

public interface MySQL {
    /**
     * Gracefully stop any current connections of checks this {@link MySQL} implementation is running.
     */
    void stop();
}