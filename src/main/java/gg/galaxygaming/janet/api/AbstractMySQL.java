package gg.galaxygaming.janet.api;

import gg.galaxygaming.janet.Janet;

import java.util.Properties;

/**
 * And abstract implementation of the {@link MySQL}.
 * This includes support basic support for gracefully stopping and for running
 * the thread that queries the database at the specified interval.
 */
public abstract class AbstractMySQL implements MySQL {
    private final long TIME = 2 * 60 * 1000;
    protected Properties properties;
    protected String url, service;

    /**
     * Runs a {@link #checkAll()} ever five minutes.
     */
    protected Thread checkThread = new Thread(() -> {
        while (true) {
            Janet.getLogger().info("Starting check (" + service + ").");
            checkAll();
            Janet.getLogger().info("Check finished (" + service + ").");
            try {
                Thread.sleep(TIME);//TODO: Check if with lower time it ever has issues that it takes too long reading the mysql tables
            } catch (InterruptedException ignored) {//It is fine if this is interrupted
            }
        }
    });

    public AbstractMySQL() {
        this.properties = new Properties();
        this.properties.setProperty("useSSL", "false");
        this.properties.setProperty("autoReconnect", "true");
        this.properties.setProperty("useLegacyDatetimeCode", "false");
        this.properties.setProperty("serverTimezone", "EST");
    }

    /**
     * Is called by the check thread and implements the different checks that each
     * implementation of {@link AbstractMySQL} performs.
     */
    protected abstract void checkAll();

    public void stop() {
        try {
            this.checkThread.interrupt();
        } catch (Exception ignored) {
        }
    }
}