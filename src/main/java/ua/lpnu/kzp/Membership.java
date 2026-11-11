package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/**
 * Base type for every gym membership.
 *
 * <p>
 * Stores the data of one CSV row that is shared by all membership kinds and
 * validates it in one place. Subtypes add only their own rules and decide how
 * much a single visit costs.
 * </p>
 */
public abstract class Membership {
    private final String client;
    private final String plan;
    private final int months;
    private final int visits;
    private final double price;
    private final MembershipKind kind;
    private final LocalDate startDate;

    /** Number of fields in one CSV row: client;plan;months;visits;price;startDate. */
    public static final int FIELD_COUNT = 6;

    /**
     * Creates a membership with validated shared fields.
     *
     * @param client    client full name, must not be blank
     * @param plan      tariff name, must not be blank
     * @param months    duration in months, at least 1
     * @param visits    number of visits, at least 1
     * @param price     total price paid, finite and not negative
     * @param startDate first day of the membership, must not be null
     * @param kind      membership category, must not be null
     * @throws IllegalArgumentException if a value breaks the rules above
     * @throws NullPointerException     if {@code startDate} or {@code kind} is null
     */
    protected Membership(String client, String plan, int months, int visits,
            double price, LocalDate startDate, MembershipKind kind) {
        // Every check runs while the arguments are evaluated, i.e. before the
        // object starts to exist. A half-built object can never escape, so
        // SpotBugs does not report CT_CONSTRUCTOR_THROW.
        this(requireText(client, "client"),
                requireText(plan, "plan"),
                requireAtLeastOne(months, "months"),
                requireAtLeastOne(visits, "visits"),
                requirePrice(price),
                Objects.requireNonNull(startDate, "startDate must not be null"),
                Objects.requireNonNull(kind, "kind must not be null"),
                true);
    }

    /*
     * Assigns values that are already validated and never throws.
     * The boolean parameter only gives this constructor a different signature.
     */
    private Membership(String client, String plan, int months, int visits,
            double price, LocalDate startDate, MembershipKind kind, boolean validated) {
        this.client = client;
        this.plan = plan;
        this.months = months;
        this.visits = visits;
        this.price = price;
        this.startDate = startDate;
        this.kind = kind;
    }

    /* Rejects null and blank text. */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /*
     * Rejects zero and negative counts; zero visits would also break visitCost().
     */
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
     * Returns the first day of the membership.
     *
     * @return start date, never null
     */
    public final LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns the first day after the membership: the start date plus its months.
     *
     * @return end date, exclusive
     */
    public final LocalDate getEndDate() {
        return startDate.plusMonths(months);
    }

    /**
     * Checks whether the membership is active on the given day.
     * The start day is included, the end day is not: a membership from
     * 2026-08-01 for three months is active until 2026-10-31.
     *
     * @param date day to check
     * @return {@code true} if {@code startDate <= date < endDate}
     * @throws NullPointerException if {@code date} is null
     */
    public final boolean isActiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !date.isBefore(startDate) && date.isBefore(getEndDate());
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
     * @return {@code true} if both objects describe the same client's membership of
     *         one kind
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

    /**
     * Returns a readable description of the membership.
     * The price is formatted with {@link Locale#ROOT}, so the output does not
     * depend on the system locale.
     *
     * @return text such as {@code MonthlyMembership{client=Іван, plan=Monthly,
     *         months=1, visits=10, price=1205.20, startDate=2026-10-01}}
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "%s{client=%s, plan=%s, months=%d, visits=%d, price=%.2f, startDate=%s}",
                getClass().getSimpleName(), client, plan, months, visits, price, startDate);
    }

    /**
     * Creates a membership of the matching subtype from one CSV row.
     *
     * @param line row in the format {@code client;plan;months;visits;price;startDate},
     *             the date in ISO format {@code yyyy-mm-dd}
     * @return monthly membership for the "Monthly" plan or a one-month term,
     *         otherwise annual
     * @throws IllegalArgumentException if the row is malformed or breaks an
     *                                  invariant
     */
    public static Membership fromCsv(String line) {
        Objects.requireNonNull(line, "line must not be null");
        String[] fields = line.split(";", -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException("expected " + FIELD_COUNT + " fields, got " + fields.length);
        }
        String client = fields[0];
        String plan = fields[1];
        int months = parseNumber(fields[2], "months");
        int visits = parseNumber(fields[3], "visits");
        double price = parseDecimal(fields[4], "price");
        LocalDate startDate = parseDate(fields[5], "startDate");

        if ("Monthly".equalsIgnoreCase(plan) || months == 1) {
            return new MonthlyMembership(client, plan, months, visits, price, startDate);
        }
        return new AnnualMembership(client, plan, months, visits, price, startDate);
    }

    /* Parses a whole number and names the field in the error message. */
    private static int parseNumber(String text, String field) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " is not a whole number: '" + text + "'", e);
        }
    }

    /* Parses a decimal number and names the field in the error message. */
    private static double parseDecimal(String text, String field) {
        try {
            return Double.parseDouble(text.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " is not a number: '" + text + "'", e);
        }
    }

    /* Parses an ISO date (yyyy-mm-dd) and names the field in the error message. */
    private static LocalDate parseDate(String text, String field) {
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " is not a date (yyyy-mm-dd): '" + text + "'", e);
        }
    }
}
