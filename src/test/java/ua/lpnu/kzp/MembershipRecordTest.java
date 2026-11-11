package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests for the export record of variant 20. */
class MembershipRecordTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2026, 11, 5);
    private static final MembershipRecord ANNA =
            new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY, START, END, 12);

    /* The record takes the client, kind, start, end and visits of a membership. */
    @Test
    void buildsRecordFromMembership() {
        Membership monthly = new MonthlyMembership("Анна Литвин", "Ранковий", 1, 12, 600.00, START);
        Membership annual = new AnnualMembership("Іван Федорчук", "Безліміт", 12, 45, 1500.50,
                LocalDate.of(2026, 1, 15));

        assertEquals(ANNA, MembershipRecord.from(monthly));
        assertEquals(new MembershipRecord("Іван Федорчук", MembershipKind.ANNUAL,
                LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15), 45), MembershipRecord.from(annual));
        assertThrows(NullPointerException.class, () -> MembershipRecord.from(null));
    }

    /* The compact constructor rejects records that break the invariants. */
    @Test
    void rejectsInvalidRecords() {
        assertThrows(IllegalArgumentException.class,
                () -> new MembershipRecord(" ", MembershipKind.MONTHLY, START, END, 12));
        assertThrows(IllegalArgumentException.class,
                () -> new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY, START, START, 12));
        assertThrows(IllegalArgumentException.class,
                () -> new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY, END, START, 12));
        assertThrows(IllegalArgumentException.class,
                () -> new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY, START, END, 0));
        assertThrows(NullPointerException.class,
                () -> new MembershipRecord("Анна Литвин", null, START, END, 12));
        assertThrows(NullPointerException.class,
                () -> new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY, START, null, 12));
    }

    /* equals and hashCode use all five fields. */
    @Test
    void comparesAllFields() {
        MembershipRecord same = new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY, START, END, 12);

        assertEquals(ANNA, same);
        assertEquals(ANNA.hashCode(), same.hashCode());
        assertNotEquals(ANNA, new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY, START, END, 13));
        assertNotEquals(ANNA, new MembershipRecord("Анна Литвин", MembershipKind.ANNUAL, START, END, 12));
    }

    /* The key is customer + start date: two memberships of one client have different keys. */
    @Test
    void keyIsCustomerAndStartDate() {
        MembershipRecord earlier = new MembershipRecord("Марія Іванівна", MembershipKind.ANNUAL,
                LocalDate.of(2025, 12, 1), LocalDate.of(2026, 6, 1), 18);
        MembershipRecord later = new MembershipRecord("Марія Іванівна", MembershipKind.ANNUAL,
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 12, 10), 20);

        assertEquals(new MembershipRecord.Key("Анна Литвин", START), ANNA.key());
        assertNotEquals(earlier.key(), later.key());
    }

    /* Fields in the exporter order give the record back. */
    @Test
    void restoresRecordFromFields() throws DataStorageException {
        assertEquals(ANNA, MembershipRecord.fromFields(
                List.of("Анна Литвин", "2026-11-05", "MONTHLY", "2026-10-05", "12")));
    }

    /* Format error: a wrong field count has no technical cause. */
    @Test
    void rejectsWrongFieldCount() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> MembershipRecord.fromFields(List.of("Анна Литвин", "2026-11-05", "MONTHLY", "2026-10-05")));

        assertEquals("expected 5 fields, got 4", error.getMessage());
        assertNull(error.getCause());
    }

    /* Value error: the NumberFormatException is kept as the cause. */
    @Test
    void rejectsBadNumberWithCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> MembershipRecord.fromFields(
                        List.of("Анна Литвин", "2026-11-05", "MONTHLY", "2026-10-05", "дванадцять")));

        assertEquals("field 'відвідування' is not a whole number: 'дванадцять'", error.getMessage());
        assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    /* Value error: an unknown enum name keeps the error of valueOf as the cause. */
    @Test
    void rejectsUnknownKindWithCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> MembershipRecord.fromFields(
                        List.of("Анна Литвин", "2026-11-05", "WEEKLY", "2026-10-05", "12")));

        assertEquals("field 'тип' is not one of [MONTHLY, ANNUAL]: 'WEEKLY'", error.getMessage());
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }

    /* Value error: a bad date keeps the DateTimeParseException as the cause. */
    @Test
    void rejectsBadDateWithCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> MembershipRecord.fromFields(
                        List.of("Анна Литвин", "2026-11-05", "MONTHLY", "05.10.2026", "12")));

        assertEquals("field 'початок' is not a date (yyyy-mm-dd): '05.10.2026'", error.getMessage());
        assertInstanceOf(DateTimeParseException.class, error.getCause());
    }

    /* Model error inside a file: the IllegalArgumentException of the constructor becomes the cause. */
    @Test
    void rejectsBrokenInvariantWithCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> MembershipRecord.fromFields(
                        List.of("Анна Литвин", "2026-10-01", "MONTHLY", "2026-10-05", "12")));

        assertTrue(error.getMessage().startsWith("invalid membership record: endDate must be after startDate"),
                error.getMessage());
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }
}
