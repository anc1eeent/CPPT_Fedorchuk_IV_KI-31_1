package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Proves that the Stream API rewrite of Lab 4 gives the same results as the loops of Lab 3.
 * The loops below are copied from the Lab 3 version (tag v1.2.1) and serve as the reference.
 */
class LoopStreamEquivalenceTest {

    private static final double PRECISION = 1e-9;

    /* The loop from MembershipSummary.from before Lab 4. */
    private static MembershipSummary summaryWithLoop(List<Membership> memberships) {
        double totalRevenue = 0.0;
        int totalVisits = 0;
        int maxMonths = 0;
        for (Membership membership : memberships) {
            totalRevenue += membership.getPrice();
            totalVisits += membership.getVisits();
            maxMonths = Math.max(maxMonths, membership.getMonths());
        }
        int count = memberships.size();
        double averageVisits = count == 0 ? 0.0 : (double) totalVisits / count;
        return new MembershipSummary(count, averageVisits, totalRevenue, maxMonths);
    }

    /* The loop from Main.formatReport before Lab 4: {total, max} visit cost. */
    private static double[] visitCostWithLoop(List<Membership> memberships) {
        double totalVisitCost = 0.0;
        double maxVisitCost = 0.0;
        for (Membership membership : memberships) {
            double cost = membership.visitCost();
            totalVisitCost += cost;
            maxVisitCost = Math.max(maxVisitCost, cost);
        }
        return new double[] {totalVisitCost, maxVisitCost};
    }

    /* The HashSet from Main.formatReport before Lab 4. */
    private static int uniqueWithHashSet(List<Membership> memberships) {
        Set<Membership> unique = new HashSet<>(memberships);
        return unique.size();
    }

    private static List<Membership> sampleData() throws IOException {
        PrintStream ignored = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        return Main.readMemberships(CommandLine.DEFAULT_INPUT, ignored);
    }

    /* Lab 1 indicators are the same for the loop and the streams on the control data. */
    @Test
    void summaryMatchesLoop() throws IOException {
        List<Membership> memberships = sampleData();
        MembershipSummary loop = summaryWithLoop(memberships);
        MembershipSummary stream = MembershipSummary.from(memberships);

        assertEquals(loop.validCount(), stream.validCount());
        assertEquals(loop.totalRevenue(), stream.totalRevenue(), PRECISION);
        assertEquals(loop.maxMonths(), stream.maxMonths());
        assertEquals(loop.averageVisits(), stream.averageVisits(), PRECISION);
    }

    /* Lab 3 visit cost indicators are the same for the loop and the streams. */
    @Test
    void visitCostMatchesLoop() throws IOException {
        List<Membership> memberships = sampleData();
        double[] loop = visitCostWithLoop(memberships);

        double streamTotal = memberships.stream().mapToDouble(Membership::visitCost).sum();
        double streamMax = memberships.stream().mapToDouble(Membership::visitCost).max().orElse(0.0);

        assertEquals(loop[0], streamTotal, PRECISION);
        assertEquals(loop[1], streamMax, PRECISION);
    }

    /* distinct() counts the same unique memberships as HashSet, the duplicate Марія included. */
    @Test
    void distinctMatchesHashSet() throws IOException {
        List<Membership> memberships = sampleData();

        assertEquals(uniqueWithHashSet(memberships), memberships.stream().distinct().count());
        assertEquals(6, memberships.stream().distinct().count());
    }

    /* An empty list gives the same zero indicators with loops and streams. */
    @Test
    void emptyInputMatchesLoop() {
        List<Membership> empty = List.of();

        assertEquals(summaryWithLoop(empty), MembershipSummary.from(empty));
        assertEquals(0, empty.stream().distinct().count());
    }

    /* The report built with streams still contains the control values recorded before Lab 4. */
    @Test
    void reportKeepsControlValues() throws IOException {
        String report = Main.formatReport(sampleData());

        for (String line : List.of(
                "Valid records: 7",
                "Total revenue: 5450.50 UAH",
                "Longest membership: 12 months",
                "Average visits: 17.57",
                "Unique clients: 6",
                "Highest visit cost: 75.00 UAH",
                "Total visit cost: 373.63 UAH")) {
            assertEquals(true, report.contains(line), line);
        }
    }
}
