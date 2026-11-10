package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Tests for the Stream API queries of {@link MembershipReport}. */
class MembershipReportTest {

    /* The valid rows of data/input.csv in file order. */
    private final List<Membership> memberships = new ArrayList<>(List.of(
            Membership.fromCsv("Іван Федорчук;Безліміт;12;45;1500.50;2026-01-15"),
            Membership.fromCsv("Петро Коваль;Ранковий;3;10;500.00;2026-08-01"),
            Membership.fromCsv("Марія Іванівна;Вечірній;6;20;900.00;2026-06-10"),
            Membership.fromCsv("Олег Савчук;Monthly;1;12;650.00;2026-10-20"),
            Membership.fromCsv("Анна Литвин;Ранковий;1;12;600.00;2026-10-05"),
            Membership.fromCsv("Ірина Бондар;Monthly;1;6;450.00;2026-11-01"),
            Membership.fromCsv("Марія Іванівна;Вечірній;6;18;850.00;2025-12-01")));

    private final List<Membership> empty = List.of();

    // --- Query 1: filter ---

    /* On the report date Петро (ends that day) and the old Марія are not active. */
    @Test
    void selectsActiveMemberships() {
        List<String> active = MembershipReport.clientNames(
                MembershipReport.activeMemberships(memberships, MembershipReport.REPORT_DATE));

        assertEquals(List.of("Іван Федорчук", "Марія Іванівна", "Олег Савчук", "Анна Литвин", "Ірина Бондар"),
                active);
    }

    /* The start day counts as active, the end day does not. */
    @Test
    void filterBoundaryIsStartInclusiveEndExclusive() {
        List<Membership> active = MembershipReport.activeMemberships(memberships, MembershipReport.REPORT_DATE);

        assertTrue(active.contains(memberships.get(5)), "Ірина starts on the report date");
        assertTrue(!active.contains(memberships.get(1)), "Петро ends on the report date");
        assertTrue(MembershipReport.activeMemberships(memberships, LocalDate.of(2026, 10, 31))
                .contains(memberships.get(1)), "Петро is still active the day before");
    }

    // --- Query 2: map ---

    /* Every membership gives its client, duplicates and order are kept. */
    @Test
    void mapsClientNames() {
        assertEquals(List.of("Іван Федорчук", "Петро Коваль", "Марія Іванівна", "Олег Савчук",
                "Анна Литвин", "Ірина Бондар", "Марія Іванівна"), MembershipReport.clientNames(memberships));
    }

    // --- Query 3: groupingBy ---

    /* Monthly: 12 + 12 + 6, annual: 45 + 10 + 20 + 18; keys follow the enum order. */
    @Test
    void sumsVisitsByKind() {
        Map<MembershipKind, Integer> visits = MembershipReport.visitsByKind(memberships);

        assertEquals(Map.of(MembershipKind.MONTHLY, 30, MembershipKind.ANNUAL, 93), visits);
        assertEquals(List.of(MembershipKind.MONTHLY, MembershipKind.ANNUAL), List.copyOf(visits.keySet()));
    }

    /* A kind without memberships has no entry. */
    @Test
    void groupHasOnlyPresentKinds() {
        Map<MembershipKind, Integer> visits = MembershipReport.visitsByKind(memberships.subList(0, 3));

        assertEquals(Map.of(MembershipKind.ANNUAL, 75), visits);
    }

    // --- Query 4: statistics ---

    /* Seven memberships with 123 visits from 6 to 45. */
    @Test
    void calculatesVisitStatistics() {
        IntSummaryStatistics stats = MembershipReport.visitStatistics(memberships);

        assertEquals(7, stats.getCount());
        assertEquals(123, stats.getSum());
        assertEquals(6, stats.getMin());
        assertEquals(45, stats.getMax());
        assertEquals(123.0 / 7, stats.getAverage(), 1e-9);
    }

    /* The statistics of the stream equal a hand-written loop over the same data. */
    @Test
    void statisticsMatchLoop() {
        int sum = 0;
        int max = Integer.MIN_VALUE;
        for (Membership membership : memberships) {
            sum += membership.getVisits();
            max = Math.max(max, membership.getVisits());
        }
        IntSummaryStatistics stats = MembershipReport.visitStatistics(memberships);

        assertEquals(sum, stats.getSum());
        assertEquals(max, stats.getMax());
    }

    // --- Query 5: top-N ---

    /* Анна and Олег have 12 visits each, so the name decides: Анна comes first. */
    @Test
    void ranksByVisitsThenClient() {
        List<String> top = MembershipReport.clientNames(
                MembershipReport.topByVisits(memberships, MembershipReport.TOP_SIZE));

        assertEquals(List.of("Іван Федорчук", "Марія Іванівна", "Марія Іванівна", "Анна Литвин", "Олег Савчук"),
                top);
        assertEquals(List.of(45, 20, 18, 12, 12), MembershipReport.topByVisits(memberships, 5).stream()
                .map(Membership::getVisits)
                .toList());
    }

    /* N = 0 gives nothing, a too large N gives everything, a negative N is rejected. */
    @Test
    void handlesBoundaryLimits() {
        assertTrue(MembershipReport.topByVisits(memberships, 0).isEmpty());
        assertEquals(7, MembershipReport.topByVisits(memberships, 100).size());
        assertThrows(IllegalArgumentException.class, () -> MembershipReport.topByVisits(memberships, -1));
    }

    // --- Search: Optional ---

    /* For a duplicate client the first membership in the list is returned. */
    @Test
    void findsFirstMembershipOfClient() {
        Optional<Membership> found = MembershipReport.findByClient(memberships, "Марія Іванівна");

        assertTrue(found.isPresent());
        assertEquals(20, found.get().getVisits());
    }

    /* A missing client gives an empty Optional instead of null. */
    @Test
    void missingClientGivesEmptyOptional() {
        assertTrue(MembershipReport.findByClient(memberships, "Невідомий клієнт").isEmpty());
    }

    // --- All queries ---

    /* Every query works on an empty list. */
    @Test
    void handlesEmptyInput() {
        assertTrue(MembershipReport.activeMemberships(empty, MembershipReport.REPORT_DATE).isEmpty());
        assertTrue(MembershipReport.clientNames(empty).isEmpty());
        assertTrue(MembershipReport.visitsByKind(empty).isEmpty());
        assertEquals(0, MembershipReport.visitStatistics(empty).getCount());
        assertTrue(MembershipReport.topByVisits(empty, 5).isEmpty());
        assertTrue(MembershipReport.findByClient(empty, "Іван Федорчук").isEmpty());
    }

    /* Queries read the list but never change its order or content. */
    @Test
    void queriesDoNotModifyInput() {
        List<Membership> snapshot = List.copyOf(memberships);

        MembershipReport.activeMemberships(memberships, MembershipReport.REPORT_DATE);
        MembershipReport.clientNames(memberships);
        MembershipReport.visitsByKind(memberships);
        MembershipReport.visitStatistics(memberships);
        MembershipReport.topByVisits(memberships, 5);
        MembershipReport.findByClient(memberships, "Анна Литвин");

        assertEquals(snapshot.size(), memberships.size());
        for (int i = 0; i < snapshot.size(); i++) {
            assertSame(snapshot.get(i), memberships.get(i));
        }
    }

    /* Null arguments are programming errors. */
    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> MembershipReport.clientNames(null));
        assertThrows(NullPointerException.class, () -> MembershipReport.activeMemberships(memberships, null));
        assertThrows(NullPointerException.class, () -> MembershipReport.findByClient(memberships, null));
    }
}
