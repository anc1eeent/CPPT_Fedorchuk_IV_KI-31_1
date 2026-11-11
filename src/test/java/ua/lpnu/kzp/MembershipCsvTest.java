package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link Membership#fromCsv(String)} and {@link Membership#toString()}. */
class MembershipCsvTest {

    /* A row with the Monthly plan becomes a monthly membership with every field kept. */
    @Test
    void parsesMonthlyRow() {
        Membership membership = Membership.fromCsv("Іван Федорчук;Monthly;1;15;1500.50;2026-10-01");

        MonthlyMembership monthly = assertInstanceOf(MonthlyMembership.class, membership);
        assertEquals("Іван Федорчук", monthly.getClient());
        assertEquals("Monthly", monthly.getPlan());
        assertEquals(1, monthly.getMonths());
        assertEquals(15, monthly.getVisits());
        assertEquals(1500.50, monthly.getPrice(), 1e-9);
        assertEquals(MembershipKind.MONTHLY, monthly.getKind());
        assertEquals(LocalDate.of(2026, 10, 1), monthly.getStartDate());
    }

    /* A longer term with another plan becomes an annual membership. */
    @Test
    void parsesAnnualRow() {
        Membership membership = Membership.fromCsv("Петро Коваль;Ранковий;3;10;500.00;2026-08-01");

        assertInstanceOf(AnnualMembership.class, membership);
        assertEquals(MembershipKind.ANNUAL, membership.getKind());
        assertEquals(3, membership.getMonths());
    }

    /* A one-month term is monthly even when the plan has another name. */
    @Test
    void oneMonthTermIsMonthly() {
        assertInstanceOf(MonthlyMembership.class, Membership.fromCsv("Олена;Разовий;1;1;150.00;2026-09-01"));
    }

    /* Spaces around numbers do not break parsing. */
    @Test
    void toleratesSpacesAroundNumbers() {
        Membership membership = Membership.fromCsv("Іван;Monthly; 1 ; 10 ; 100.0 ; 2026-09-01 ");

        assertEquals(10, membership.getVisits());
        assertEquals(100.0, membership.getPrice(), 1e-9);
    }

    /* Every kind of malformed or invalid row is rejected with IllegalArgumentException. */
    @ParameterizedTest
    @ValueSource(strings = {
        "Іван;Monthly;1;10;100",                   // too few fields
        "Іван;Monthly;1;10;100;2026-09-01;extra",  // too many fields
        "Іван;Monthly;помилка;10;100;2026-09-01",  // months is not a number
        "Іван;Monthly;1;10;abc;2026-09-01",        // price is not a number
        ";Monthly;1;10;100;2026-09-01",            // empty client
        "Іван;;1;10;100;2026-09-01",               // empty plan
        "Іван;Monthly;1;0;100;2026-09-01",         // zero visits
        "Іван;Monthly;1;10;-5;2026-09-01",         // negative price
        "Іван;Annual;13;10;100;2026-09-01",        // annual term longer than 12 months
        "Іван;Monthly;1;10;100;2026-13-01",        // month 13 does not exist
        "Іван;Monthly;1;10;100;01.09.2026",        // date not in ISO format
        "Іван;Monthly;1;10;100;"                   // empty date
    })
    void rejectsInvalidRows(String line) {
        assertThrows(IllegalArgumentException.class, () -> Membership.fromCsv(line));
    }

    /* The error message names the broken field, so the report can explain the skipped line. */
    @Test
    void errorMessageNamesTheField() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Membership.fromCsv("Олена;Разовий;помилка;1;150.00;2026-09-01"));

        assertTrue(error.getMessage().contains("months"), error.getMessage());
    }

    /* An invalid date names the field and the rejected text. */
    @Test
    void invalidDateNamesTheField() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Membership.fromCsv("Богдан;Денний;3;15;480.00;2026-13-01"));

        assertEquals("startDate is not a date (yyyy-mm-dd): '2026-13-01'", error.getMessage());
    }

    /* A null line is a programming error, not bad data. */
    @Test
    void rejectsNullLine() {
        assertThrows(NullPointerException.class, () -> Membership.fromCsv(null));
    }

    /* toString shows the subtype and keeps a dot in the price even under a locale that uses a comma. */
    @Test
    void toStringUsesRootLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"));

            assertEquals(
                    "MonthlyMembership{client=Іван, plan=Monthly, months=1, visits=10, price=1205.20, startDate=2026-10-01}",
                    Membership.fromCsv("Іван;Monthly;1;10;1205.2;2026-10-01").toString());
        } finally {
            Locale.setDefault(saved);
        }
    }
}
