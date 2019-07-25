package gg.galaxygaming.janet.forums.donations;

import gg.galaxygaming.janet.api.AbstractIntegration;

/**
 * An implementation of {@link gg.galaxygaming.janet.api.Integration} to handle donations made on the forums.
 */
public class DonationIntegration extends AbstractIntegration {
    public DonationIntegration() {
        super();
        this.mysql = new DonationMySQL();
    }
}