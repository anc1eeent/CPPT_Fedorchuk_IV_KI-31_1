package ua.lpnu.kzp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stream API queries of variant 20 over a list of memberships.
 *
 * <p>Every method only reads the given list: it never changes its order or content,
 * and it does not print anything. Formatting the results is the job of the report.</p>
 */
public final class MembershipReport {

    /**
     * Day on which active memberships are counted.
     * It is fixed instead of {@code LocalDate.now()}, so the report and the tests
     * give the same result on any day.
     */
    public static final LocalDate REPORT_DATE = LocalDate.of(2026, 11, 1);

    /** Default number of entries in the top list. */
    public static final int TOP_SIZE = 5;

    /** Orders by visits from the highest; equal visits are ordered by client name. */
    public static final Comparator<Membership> BY_VISITS_DESC_THEN_CLIENT = Comparator
            .comparingInt(Membership::getVisits)
            .reversed()
            .thenComparing(Membership::getClient);

    private MembershipReport() {
    }

    /**
     * Query 1 (filter): selects memberships that are active on the given day.
     *
     * @param memberships memberships to check
     * @param date        day to check, usually {@link #REPORT_DATE}
     * @return active memberships in the original order
     * @throws NullPointerException if an argument is null
     */
    public static List<Membership> activeMemberships(List<Membership> memberships, LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return streamOf(memberships)
                .filter(membership -> membership.isActiveOn(date))
                .toList();
    }

    /**
     * Query 2 (map): returns the client of every membership.
     * A client with two memberships appears twice.
     *
     * @param memberships memberships to read
     * @return client names in the original order
     * @throws NullPointerException if {@code memberships} is null
     */
    public static List<String> clientNames(List<Membership> memberships) {
        return streamOf(memberships)
                .map(Membership::getClient)
                .toList();
    }

    /**
     * Query 3 (groupingBy): sums the visits of each membership kind.
     *
     * @param memberships memberships to group
     * @return visits per kind; a kind without memberships has no entry
     * @throws NullPointerException if {@code memberships} is null
     */
    public static Map<MembershipKind, Integer> visitsByKind(List<Membership> memberships) {
        return streamOf(memberships)
                .collect(Collectors.groupingBy(
                        Membership::getKind,
                        () -> new EnumMap<>(MembershipKind.class),
                        Collectors.summingInt(Membership::getVisits)));
    }

    /**
     * Query 4 (statistics): count, sum, minimum, maximum and average of visits in one pass.
     *
     * <p>For an empty list the count, sum and average are zero, while the minimum is
     * {@link Integer#MAX_VALUE} and the maximum is {@link Integer#MIN_VALUE}; check
     * {@code getCount()} before showing them.</p>
     *
     * @param memberships memberships to measure
     * @return visit statistics
     * @throws NullPointerException if {@code memberships} is null
     */
    public static IntSummaryStatistics visitStatistics(List<Membership> memberships) {
        return streamOf(memberships)
                .collect(Collectors.summarizingInt(Membership::getVisits));
    }

    /**
     * Query 5 (top-N): memberships with the most visits; equal visits by client name.
     *
     * @param memberships memberships to rank
     * @param limit       how many entries to return, not negative
     * @return at most {@code limit} memberships, best first
     * @throws IllegalArgumentException if {@code limit} is negative
     * @throws NullPointerException     if {@code memberships} is null
     */
    public static List<Membership> topByVisits(List<Membership> memberships, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative, got " + limit);
        }
        // sorted() before limit(): first order everything, then keep the best.
        return streamOf(memberships)
                .sorted(BY_VISITS_DESC_THEN_CLIENT)
                .limit(limit)
                .toList();
    }

    /**
     * Search (Optional): finds the first membership of the given client.
     *
     * @param memberships memberships to search
     * @param client      exact client name
     * @return the first matching membership, or an empty {@code Optional}
     * @throws NullPointerException if an argument is null
     */
    public static Optional<Membership> findByClient(List<Membership> memberships, String client) {
        Objects.requireNonNull(client, "client must not be null");
        return streamOf(memberships)
                .filter(membership -> membership.getClient().equals(client))
                .findFirst();
    }

    /* Rejects a null list and opens a stream over it. */
    private static Stream<Membership> streamOf(List<Membership> memberships) {
        return Objects.requireNonNull(memberships, "memberships must not be null").stream();
    }
}
