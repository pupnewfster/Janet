package gg.galaxygaming.janet.base;

import gg.galaxygaming.janet.Janet;

import java.util.Properties;

public abstract class AbstractMySQL implements MySQL {
    protected Properties properties;
    protected String url, service;

    protected Thread checkThread = new Thread(() -> {
        while (true) {
            if (Janet.DEBUG)
                System.out.println("[DEBUG] Starting check (" + service + ").");
            checkAll();
            if (Janet.DEBUG)
                System.out.println("[DEBUG] Check finished (" + service + ").");
            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException ignored) {//It is fine if this is interupted
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

    protected abstract void checkAll();

    public void stop() {
        try {
            this.checkThread.interrupt();
        } catch (Exception ignored) {
        }
    }
}