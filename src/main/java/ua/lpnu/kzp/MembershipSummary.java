package ua.lpnu.kzp;

import java.util.List;
import java.util.Objects;

/**
 * Report indicators of variant 20 calculated over all valid memberships.
 *
 * @param validCount    number of valid records, not negative
 * @param averageVisits average number of visits per membership, finite and not negative
 * @param totalRevenue  sum of all membership prices, finite and not negative
 * @param maxMonths     duration of the longest membership in months, not negative
 */
public record MembershipSummary(
        int validCount,
        double averageVisits,
        double totalRevenue,
        int maxMonths) {

    /**
     * Validates the components. Java assigns the fields itself after this block.
     *
     * @throws IllegalArgumentException if a component is negative or not finite
     */
    public MembershipSummary {
        requireNonNegative(validCount, "validCount");
        requireFiniteNonNegative(averageVisits, "averageVisits");
        requireFiniteNonNegative(totalRevenue, "totalRevenue");
        requireNonNegative(maxMonths, "maxMonths");
    }

    /**
     * Calculates the indicators of variant 20 for the given memberships.
     *
     * @param memberships valid memberships, may be empty
     * @return summary; every value is zero for an empty list
     * @throws NullPointerException if {@code memberships} is null
     */
    public static MembershipSummary from(List<Membership> memberships) {
        Objects.requireNonNull(memberships, "memberships must not be null");
        double totalRevenue = 0.0;
        int totalVisits = 0;
        int maxMonths = 0;
        for (Membership membership : memberships) {
            totalRevenue += membership.getPrice();
            totalVisits += membership.getVisits();
            maxMonths = Math.max(maxMonths, membership.getMonths());
        }
        int count = memberships.size();
        // Without the cast int / int would drop the fraction: 76 / 3 == 25, not 25.33.
        double averageVisits = count == 0 ? 0.0 : (double) totalVisits / count;
        return new MembershipSummary(count, averageVisits, totalRevenue, maxMonths);
    }

    /* Rejects negative counts. */
    private static void requireNonNegative(int value, String component) {
        if (value < 0) {
            throw new IllegalArgumentException(component + " must not be negative, got " + value);
        }
    }

    /* Rejects negative values, NaN and infinity. */
    private static void requireFiniteNonNegative(double value, String component) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    component + " must be finite and not negative, got " + value);
        }
    }
}
