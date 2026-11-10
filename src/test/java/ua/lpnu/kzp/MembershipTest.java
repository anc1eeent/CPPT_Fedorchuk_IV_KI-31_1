package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Tests for the membership hierarchy: validation, polymorphism and equality. */
class MembershipTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);

    /* A valid monthly membership keeps its client and kind. */
    @Test
    void testValidMonthlyMembershipCreation() {
        MonthlyMembership membership = new MonthlyMembership("Іван", "Monthly", 1, 10, 1205.2, START);

        assertEquals("Іван", membership.getClient());
        assertEquals(MembershipKind.MONTHLY, membership.getKind());
    }

    /* The base type rejects a negative price for every subtype. */
    @Test
    void testNegativePriceThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnnualMembership("Олег", "Annual", 12, 10, -1500.0, START));
    }

    /* The base type rejects a blank client name. */
    @Test
    void testBaseTypeRejectsInvalidCommonState() {
        assertThrows(IllegalArgumentException.class,
                () -> new MonthlyMembership("", "Monthly", 1, 10, 1500.0, START));
    }

    /* The same polymorphic call gives different results for different subtypes. */
    @Test
    void testSubtypesCalculateCostDifferently() {
        Membership monthly = new MonthlyMembership("Олег", "Monthly", 1, 10, 1500.0, START);
        Membership annual = new AnnualMembership("Марія", "Annual", 12, 120, 12000.0, START);

        assertEquals(150.0, monthly.visitCost(), 1e-9);  // 1500 / 10
        assertEquals(100.0, annual.visitCost(), 1e-9);   // (12000 / 12) / (120 / 12)
        assertNotEquals(monthly.visitCost(), annual.visitCost());
    }

    /* Memberships of one subtype and client are equal, so a HashSet keeps one. */
    @Test
    void testEqualObjectsHaveOneSetEntry() {
        Set<Membership> uniqueMemberships = new HashSet<>();

        uniqueMemberships.add(new MonthlyMembership("Олег", "Monthly", 1, 10, 1500.0, START));
        uniqueMemberships.add(new MonthlyMembership("Олег", "Monthly", 1, 12, 1700.0, START));

        assertEquals(1, uniqueMemberships.size());
    }

    /* Different subtypes are never equal, even for the same client. */
    @Test
    void testDifferentSubtypesAreNotEqual() {
        Membership monthly = new MonthlyMembership("Олег", "Monthly", 1, 10, 1500.0, START);
        Membership annual = new AnnualMembership("Олег", "Annual", 12, 10, 1500.0, START);

        assertNotEquals(monthly, annual);
    }

    /* The end date is the start date plus the number of months. */
    @Test
    void endDateAddsMonthsToStart() {
        Membership membership = new AnnualMembership("Петро", "Ранковий", 3, 10, 500.0, START);

        assertEquals(LocalDate.of(2026, 12, 1), membership.getEndDate());
    }

    /* Active means start <= date < end: the start day counts, the end day does not. */
    @Test
    void activeFromStartDayUntilTheDayBeforeEnd() {
        Membership membership = new AnnualMembership("Петро", "Ранковий", 3, 10, 500.0, START);

        assertFalse(membership.isActiveOn(START.minusDays(1)));
        assertTrue(membership.isActiveOn(START));
        assertTrue(membership.isActiveOn(LocalDate.of(2026, 11, 30)));
        assertFalse(membership.isActiveOn(LocalDate.of(2026, 12, 1)));
    }

    /* A missing start date is a programming error. */
    @Test
    void rejectsNullStartDate() {
        assertThrows(NullPointerException.class,
                () -> new MonthlyMembership("Олег", "Monthly", 1, 10, 1500.0, null));
    }
}
