package gg.galaxygaming.janetissuetracker.base;

import gg.galaxygaming.janetissuetracker.Janet;

import java.util.Properties;

public abstract class AbstractMySQL implements MySQL {
    protected Properties properties;
    protected String url, service;


    protected Thread checkThread = new Thread(() -> {
        while (true) {
            if (Janet.DEBUG)
                System.out.println("[DEBUG] Starting user check (" + service + ").");
            checkAll();
            if (Janet.DEBUG)
                System.out.println("[DEBUG] User check finished (" + service + ").");
            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
}