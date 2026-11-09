package ua.lpnu.kzp;

import java.util.Objects;

/**
 * Base type for every gym membership.
 *
 * <p>Stores the data of one CSV row that is shared by all membership kinds and
 * validates it in one place. Subtypes add only their own rules and decide how
 * much a single visit costs.</p>
 */
public abstract class Membership {
    private final String client;
    private final String plan;
    private final int months;
    private final int visits;
    private final double price;
    private final MembershipKind kind;

    /**
     * Creates a membership with validated shared fields.
     *
     * @param client client full name, must not be blank
     * @param plan   tariff name, must not be blank
     * @param months duration in months, at least 1
     * @param visits number of visits, at least 1
     * @param price  total price paid, finite and not negative
     * @param kind   membership category, must not be null
     * @throws IllegalArgumentException if a value breaks the rules above
     * @throws NullPointerException     if {@code kind} is null
     */
    protected Membership(String client, String plan, int months, int visits,
            double price, MembershipKind kind) {
        // Every check runs while the arguments are evaluated, i.e. before the
        // object starts to exist. A half-built object can never escape, so
        // SpotBugs does not report CT_CONSTRUCTOR_THROW.
        this(requireText(client, "client"),
                requireText(plan, "plan"),
                requireAtLeastOne(months, "months"),
                requireAtLeastOne(visits, "visits"),
                requirePrice(price),
                Objects.requireNonNull(kind, "kind must not be null"),
                true);
    }

    /*
     * Assigns values that are already validated and never throws.
     * The boolean parameter only gives this constructor a different signature.
     */
    private Membership(String client, String plan, int months, int visits,
            double price, MembershipKind kind, boolean validated) {
        this.client = client;
        this.plan = plan;
        this.months = months;
        this.visits = visits;
        this.price = price;
        this.kind = kind;
    }

    /* Rejects null and blank text. */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /* Rejects zero and negative counts; zero visits would also break visitCost(). */
    private static int requireAtLeastOne(int value, String field) {
        if (value < 1) {
            throw new IllegalArgumentException(field + " must be at least 1, got " + value);
        }
        return value;
    }

    /* Rejects negative prices, NaN and infinity. */
    private static double requirePrice(double price) {
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("price must be a finite non-negative number, got " + price);
        }
        return price;
    }

    /**
     * Returns the client full name.
     *
     * @return client name, never blank
     */
    public final String getClient() {
        return client;
    }

    /**
     * Returns the tariff name from the CSV file.
     *
     * @return tariff name, never blank
     */
    public final String getPlan() {
        return plan;
    }

    /**
     * Returns the membership duration.
     *
     * @return number of months, at least 1
     */
    public final int getMonths() {
        return months;
    }

    /**
     * Returns the total number of visits.
     *
     * @return number of visits, at least 1
     */
    public final int getVisits() {
        return visits;
    }

    /**
     * Returns the total price paid for the membership.
     *
     * @return price, not negative
     */
    public final double getPrice() {
        return price;
    }

    /**
     * Returns the membership category.
     *
     * @return category, never null
     */
    public final MembershipKind getKind() {
        return kind;
    }

    /**
     * Calculates the cost of a single visit.
     * Each subtype applies its own rule, so callers never check the type.
     *
     * @return cost of one visit
     */
    public abstract double visitCost();

    /**
     * Two memberships are equal when they have the same subtype and client.
     *
     * @param other object to compare with
     * @return {@code true} if both objects describe the same client's membership of one kind
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // getClass() instead of instanceof: a monthly and an annual membership
        // of the same client are different objects.
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Membership that = (Membership) other;
        return client.equals(that.client);
    }

    /**
     * Returns a hash code built from the same fields as {@link #equals(Object)}.
     *
     * @return hash code
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), client);
    }
}
