package gg.galaxygaming.janet.api;

/**
 * A api interface for the integrations {@link gg.galaxygaming.janet.Janet} includes.
 */
public interface Integration {
    /**
     * Retrieves the {@link MySQL} implementation this {@link Integration} uses.
     * @return The instance of the {@link MySQL} implementation for this {@link Integration}.
     */
    MySQL getMySQL();

    /**
     * Gracefully stop this {@link Integration}.
     */
    void stop();
}