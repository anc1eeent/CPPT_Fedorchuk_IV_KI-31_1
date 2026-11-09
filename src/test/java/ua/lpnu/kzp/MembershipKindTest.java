package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests for the {@link MembershipKind} enum. */
class MembershipKindTest {

    /* The enum has exactly two categories in a fixed order. */
    @Test
    void hasTwoCategoriesInOrder() {
        assertEquals(List.of(MembershipKind.MONTHLY, MembershipKind.ANNUAL),
                List.of(MembershipKind.values()));
    }

    /* Every category has a readable label for reports. */
    @Test
    void providesLabels() {
        assertEquals("Monthly", MembershipKind.MONTHLY.label());
        assertEquals("Annual", MembershipKind.ANNUAL.label());
    }

    /* valueOf finds a constant by its exact name and rejects unknown names. */
    @Test
    void parsesConstantNames() {
        assertEquals(MembershipKind.ANNUAL, MembershipKind.valueOf("ANNUAL"));
        assertThrows(IllegalArgumentException.class, () -> MembershipKind.valueOf("WEEKLY"));
    }

    /* Each subtype reports its own category, so callers never check the class. */
    @Test
    void subtypesReportTheirKind() {
        assertEquals(MembershipKind.MONTHLY,
                new MonthlyMembership("Іван", "Monthly", 1, 10, 100.0).getKind());
        assertEquals(MembershipKind.ANNUAL,
                new AnnualMembership("Іван", "Annual", 12, 10, 100.0).getKind());
    }
}
