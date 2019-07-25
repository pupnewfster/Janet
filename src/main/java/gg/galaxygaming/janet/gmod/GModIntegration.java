package gg.galaxygaming.janet.gmod;

import gg.galaxygaming.janet.api.AbstractIntegration;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.Integration} to connect to the GMod servers.
 * <p>
 * At the moment this is just a wrapper for loading and running {@link GModMySQL}.
 */
public class GModIntegration extends AbstractIntegration {//TODO: Maybe do more things or move some of JanetGMod into here

    public GModIntegration() {
        super();
        this.mysql = new GModMySQL();
    }
}