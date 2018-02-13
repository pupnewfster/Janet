package gg.galaxygaming.janetissuetracker.base;

public abstract class AbstractIntegration implements Integration {
    protected MySQL mysql;

    public MySQL getMySQL() {
        return this.mysql;
    }

    public void stop() {
        if (this.mysql != null)
            this.mysql.stop();
    }
}