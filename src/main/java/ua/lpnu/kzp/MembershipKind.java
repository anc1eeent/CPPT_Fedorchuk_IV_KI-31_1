package ua.lpnu.kzp;

/** Category of a gym membership. */
public enum MembershipKind {
    /** Membership for a single month. */
    MONTHLY("Monthly"),
    /** Membership for several months, up to a year. */
    ANNUAL("Annual");

    private final String label;

    /* Called only for the constants declared above. */
    MembershipKind(String label) {
        this.label = label;
    }

    /**
     * Returns a human-readable name for reports.
     *
     * @return category label
     */
    public String label() {
        return label;
    }
}
