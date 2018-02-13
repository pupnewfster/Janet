package gg.galaxygaming.janet.GMod;

import gg.galaxygaming.janet.base.AbstractIntegration;

public class GModIntegration extends AbstractIntegration {//TODO: Maybe do more things or move some of JanetGMod into here
    public GModIntegration() {
        this.mysql = new GModMySQL();
    }
}