package gg.galaxygaming.janet.api;

/**
 * And abstract implementation of the {@link Integration}.
 * This includes support basic support for gracefully stopping
 */
public abstract class AbstractIntegration implements Integration {//TODO: Maybe move some stuff to a MySQLIntegration abstraction
    protected MySQL mysql;

    public MySQL getMySQL() {
        return this.mysql;
    }

    public void stop() {
        if (this.mysql != null)
            this.mysql.stop();
    }
}