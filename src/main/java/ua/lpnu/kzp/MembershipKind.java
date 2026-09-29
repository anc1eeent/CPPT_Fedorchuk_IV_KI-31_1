package ua.lpnu.kzp;

/** Категорія абонемента. */
public enum MembershipKind {
    MONTHLY("Місячний"),
    ANNUAL("Річний");

    private final String label;

    MembershipKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}