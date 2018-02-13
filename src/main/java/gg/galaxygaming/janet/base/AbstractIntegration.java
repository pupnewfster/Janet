package gg.galaxygaming.janet.base;

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