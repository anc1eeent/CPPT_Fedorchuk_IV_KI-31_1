package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for saving membership records to CSV and reading them back. */
class MembershipStorageTest {

    private static final String HEADER = "клієнт,кінець,тип,початок,відвідування";

    private static final MembershipRecord ANNA = new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY,
            LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 5), 12);

    /* The test file of variant 20: six records, one of them with a comma, quotes and a line break. */
    @Test
    void loadsTestFileWithSpecialCharacters() throws Exception {
        Path file = resource("memberships.csv");

        List<MembershipRecord> records = MembershipStorage.load(file);

        assertEquals(6, records.size());
        assertEquals(8, Files.readAllLines(file, StandardCharsets.UTF_8).size(), "physical lines");
        assertEquals(7, CsvParser.read(file).size(), "CSV records with the header");
        String petro = records.get(1).customer();
        assertTrue(petro.startsWith("Коваль, Петро \"Ранковий\""), petro);
        assertEquals(2L, petro.lines().count(), petro);
        assertEquals(ANNA, records.get(4));
    }

    /* file → objects → file → objects gives equal lists, and a record is found by its key. */
    @Test
    void roundTripOfTestFileGivesEqualRecords(@TempDir Path folder) throws Exception {
        List<MembershipRecord> original = MembershipStorage.load(resource("memberships.csv"));

        MembershipStorage.RoundTrip result = MembershipStorage.roundTrip(folder.resolve("copy.csv"), original);

        assertTrue(result.isEqual());
        assertEquals(original, result.restored());
        assertEquals(7, result.csvRecords());

        Repository<MembershipRecord> restored = new Repository<>();
        restored.addAll(result.restored());
        assertEquals(List.of(ANNA), restored.find(record -> record.key().equals(ANNA.key())));
    }

    /* A multiline value with a comma and quotes survives the round-trip unchanged. */
    @Test
    void multilineFieldSurvivesRoundTrip(@TempDir Path folder) throws DataStorageException {
        MembershipRecord tricky = new MembershipRecord("Коваль, Петро \"Ранковий\"\nкорпоративний клієнт",
                MembershipKind.ANNUAL, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1), 10);

        MembershipStorage.RoundTrip result =
                MembershipStorage.roundTrip(folder.resolve("tricky.csv"), List.of(tricky, ANNA));

        assertTrue(result.isEqual());
        assertEquals(tricky.customer(), result.restored().get(0).customer());
        assertEquals(3, result.csvRecords());
    }

    /* An empty list is saved as the header only and read back as an empty list. */
    @Test
    void roundTripOfEmptyList(@TempDir Path folder) throws DataStorageException, IOException {
        Path file = folder.resolve("empty.csv");

        MembershipStorage.RoundTrip result = MembershipStorage.roundTrip(file, List.of());

        assertTrue(result.isEqual());
        assertEquals(1, result.csvRecords());
        assertEquals(HEADER + System.lineSeparator(), Files.readString(file, StandardCharsets.UTF_8));
    }

    /* An empty file is valid and gives no records. */
    @Test
    void emptyFileGivesNoRecords() throws Exception {
        assertEquals(List.of(), MembershipStorage.load(resource("empty.csv")));
    }

    /* A file with only the header is valid and gives no records. */
    @Test
    void headerOnlyGivesNoRecords(@TempDir Path folder) throws IOException, DataStorageException {
        Path file = Files.writeString(folder.resolve("header.csv"), HEADER + "\n", StandardCharsets.UTF_8);

        assertEquals(List.of(), MembershipStorage.load(file));
    }

    /* A broken row is reported with the file and record number; the root cause is kept. */
    @Test
    void brokenRowNamesRecordAndKeepsCause() throws Exception {
        Path file = resource("broken-row.csv");

        DataStorageException error = assertThrows(DataStorageException.class, () -> MembershipStorage.load(file));

        assertEquals("cannot read record 2 of " + file
                + ": field 'відвідування' is not a whole number: 'дванадцять'", error.getMessage());
        DataStorageException recordError = assertInstanceOf(DataStorageException.class, error.getCause());
        assertInstanceOf(NumberFormatException.class, recordError.getCause());
    }

    /* A record with a wrong field count is a format error. */
    @Test
    void rejectsWrongFieldCount(@TempDir Path folder) throws IOException {
        Path file = Files.writeString(folder.resolve("short.csv"),
                HEADER + "\nАнна Литвин,2026-11-05,MONTHLY,2026-10-05\n", StandardCharsets.UTF_8);

        DataStorageException error = assertThrows(DataStorageException.class, () -> MembershipStorage.load(file));

        assertTrue(error.getMessage().endsWith("record 1 of " + file + ": expected 5 fields, got 4"),
                error.getMessage());
    }

    /* A file with other columns is rejected before any record is parsed. */
    @Test
    void rejectsUnexpectedHeader(@TempDir Path folder) throws IOException {
        Path file = Files.writeString(folder.resolve("other.csv"),
                "клієнт,тип,початок,кінець,відвідування\n", StandardCharsets.UTF_8);

        DataStorageException error = assertThrows(DataStorageException.class, () -> MembershipStorage.load(file));

        assertTrue(error.getMessage().startsWith("unexpected header in " + file), error.getMessage());
    }

    /* File error: a missing file keeps the IOException as the cause. */
    @Test
    void reportsMissingFile(@TempDir Path folder) {
        Path missing = folder.resolve("missing.csv");

        DataStorageException error = assertThrows(DataStorageException.class, () -> MembershipStorage.load(missing));

        assertInstanceOf(IOException.class, error.getCause());
    }

    /* Returns the path of a file in src/test/resources/csv. */
    private static Path resource(String name) throws URISyntaxException {
        URL url = MembershipStorageTest.class.getResource("/csv/" + name);
        if (url == null) {
            throw new IllegalStateException("missing test resource csv/" + name);
        }
        return Path.of(url.toURI());
    }
}
