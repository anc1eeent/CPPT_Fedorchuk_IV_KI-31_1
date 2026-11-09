package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
