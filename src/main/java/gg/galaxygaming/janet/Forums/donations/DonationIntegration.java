package gg.galaxygaming.janet.Forums.donations;

import gg.galaxygaming.janet.base.AbstractIntegration;

public class DonationIntegration extends AbstractIntegration {
    public DonationIntegration() {
        super();
        this.mysql = new DonationMySQL();
    }
}