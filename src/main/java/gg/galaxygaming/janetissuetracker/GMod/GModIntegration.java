package gg.galaxygaming.janetissuetracker.GMod;

import gg.galaxygaming.janetissuetracker.base.AbstractIntegration;

public class GModIntegration extends AbstractIntegration {//TODO: Maybe do more things or move some of JanetGMod into here
    public GModIntegration() {
        this.mysql = new GModMySQL();
    }
}