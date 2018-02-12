package gg.galaxygaming.janetissuetracker.GMod;

public class GModIntegration {//TODO: Maybe do more things or move some of JanetGMod into here
    private GModMySQL mysql;

    public GModIntegration() {
        this.mysql = new GModMySQL();
    }

    public void stop() {
        if (this.mysql != null)
            this.mysql.stop();
    }
}