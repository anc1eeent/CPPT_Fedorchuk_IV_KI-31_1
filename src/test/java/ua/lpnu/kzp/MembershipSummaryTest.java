package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests for the {@link MembershipSummary} record. */
class MembershipSummaryTest {

    /* Record components are available through the generated accessors. */
    @Test
    void exposesComponents() {
        MembershipSummary summary = new MembershipSummary(3, 25.0, 2900.50, 12);

        assertEquals(3, summary.validCount());
        assertEquals(25.0, summary.averageVisits(), 1e-9);
        assertEquals(2900.50, summary.totalRevenue(), 1e-9);
        assertEquals(12, summary.maxMonths());
    }

    /* Records with equal components are equal and have equal hash codes. */
    @Test
    void equalComponentsMeanEqualRecords() {
        MembershipSummary first = new MembershipSummary(3, 25.0, 2900.50, 12);
        MembershipSummary second = new MembershipSummary(3, 25.0, 2900.50, 12);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    /* An empty data set gives an all-zero summary, which is valid. */
    @Test
    void allowsZeroSummary() {
        assertDoesNotThrow(() -> new MembershipSummary(0, 0.0, 0.0, 0));
    }

    /* Negative and non-finite components are rejected. */
    @Test
    void rejectsInvalidComponents() {
        assertThrows(IllegalArgumentException.class, () -> new MembershipSummary(-1, 0.0, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MembershipSummary(0, -1.0, 0.0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new MembershipSummary(0, Double.POSITIVE_INFINITY, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MembershipSummary(0, 0.0, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new MembershipSummary(0, 0.0, -0.01, 0));
        assertThrows(IllegalArgumentException.class, () -> new MembershipSummary(0, 0.0, 0.0, -1));
    }

    /* The sample data of Lab 1 gives the same indicators as the Lab 1 report. */
    @Test
    void fromMatchesLab1Report() {
        List<Membership> memberships = List.of(
                Membership.fromCsv("Іван Федорчук;Безліміт;12;45;1500.50"),
                Membership.fromCsv("Петро Коваль;Ранковий;3;10;500.00"),
                Membership.fromCsv("Марія Іванівна;Вечірній;6;20;900.00"));

        assertEquals(new MembershipSummary(3, 25.0, 2900.50, 12), MembershipSummary.from(memberships));
    }

    /* The average keeps its fraction instead of being truncated by integer division. */
    @Test
    void fromKeepsFractionalAverage() {
        List<Membership> memberships = List.of(
                Membership.fromCsv("A;Monthly;1;25;100"),
                Membership.fromCsv("B;Monthly;1;25;100"),
                Membership.fromCsv("C;Monthly;1;26;100"));

        assertEquals(76.0 / 3, MembershipSummary.from(memberships).averageVisits(), 1e-9);
    }

    /* An empty list gives zeros instead of dividing by zero. */
    @Test
    void fromEmptyListGivesZeros() {
        assertEquals(new MembershipSummary(0, 0.0, 0.0, 0), MembershipSummary.from(List.of()));
    }

    /* A null list is a programming error. */
    @Test
    void fromRejectsNull() {
        assertThrows(NullPointerException.class, () -> MembershipSummary.from(null));
    }
}
