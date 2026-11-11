package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for the command-line entry point. */
class MainTest {

    private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

    /* Tests run from compiled classes, not from the JAR, so there is no manifest version. */
    @Test
    void versionFallsBackToDevOutsideJar() {
        assertEquals("dev", Main.appVersion());
    }

    /* --help prints the usage and does not process any data. */
    @Test
    void helpPrintsUsage() {
        assertEquals(Main.EXIT_OK, Main.run(new String[] {"--help"}, out, err));
        assertEquals(CommandLine.USAGE, output());
    }

    /* --version prints only the version line. */
    @Test
    void versionPrintsOneLine() {
        assertEquals(Main.EXIT_OK, Main.run(new String[] {"--version"}, out, err));
        assertEquals("Gym Membership Report dev", output().strip());
    }

    /* An invalid option prints the error and the usage to the error stream. */
    @Test
    void invalidOptionPrintsErrorAndUsage() {
        assertEquals(Main.EXIT_USAGE, Main.run(new String[] {"--colour"}, out, err));
        assertTrue(errors().contains("unknown option: --colour"), errors());
        assertTrue(errors().contains("Usage:"), errors());
        assertEquals("", output());
    }

    /* A full run reads the given CSV, reports skipped lines and writes the report file. */
    @Test
    void processesInputAndWritesReport(@TempDir Path folder) throws IOException {
        Path input = folder.resolve("input.csv");
        Path output = folder.resolve("nested").resolve("report.txt");
        Path export = folder.resolve("nested").resolve("memberships.csv");
        Files.write(input, List.of(
                "Іван Федорчук;Безліміт;12;45;1500.50;2026-01-15",
                "Петро Коваль;Ранковий;3;10;500.00;2026-08-01",
                "Олена Петрівна;Разовий;помилка;1;150.00;2026-09-01",
                "Марія Іванівна;Вечірній;6;20;900.00;2026-06-10"), StandardCharsets.UTF_8);

        int exitCode = Main.run(new String[] {"--input", input.toString(), "--output", output.toString(),
            "--export", export.toString()}, out, err);

        assertEquals(Main.EXIT_OK, exitCode);
        String report = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(report.contains("Valid records: 3"), report);
        assertTrue(report.contains("Total revenue: 2900.50 UAH"), report);
        assertTrue(report.contains("Total visit cost: 147.24 UAH"), report);
        assertTrue(report.contains("--- STREAM QUERIES (report date 2026-11-01) ---"), report);
        assertTrue(report.contains("Visits by kind: Annual=75"), report);
        assertTrue(output().startsWith(report), output());
        assertTrue(errors().contains("Skipped line 3: months is not a whole number: 'помилка'"), errors());
        assertFalse(report.contains("CSV EXPORT"), "the report file keeps the format of Lab 4");
    }

    /* The run exports the valid memberships to CSV, reads them back and prints the round-trip. */
    @Test
    void exportsRecordsAndPrintsRoundTrip(@TempDir Path folder) throws IOException, DataStorageException {
        Path input = folder.resolve("input.csv");
        Path export = folder.resolve("out").resolve("memberships.csv");
        Files.write(input, List.of(
                "Анна Литвин;Ранковий;1;12;600.00;2026-10-05",
                "Петро Коваль;Ранковий;3;10;500.00;2026-08-01",
                "Олена Петрівна;Разовий;помилка;1;150.00;2026-09-01"), StandardCharsets.UTF_8);

        int exitCode = Main.run(new String[] {"--input", input.toString(),
            "--output", folder.resolve("report.txt").toString(), "--export", export.toString()}, out, err);

        assertEquals(Main.EXIT_OK, exitCode);
        assertEquals(List.of(
                "клієнт,кінець,тип,початок,відвідування",
                "Анна Литвин,2026-11-05,MONTHLY,2026-10-05,12",
                "Петро Коваль,2026-11-01,ANNUAL,2026-08-01,10"),
                Files.readAllLines(export, StandardCharsets.UTF_8));
        assertEquals(List.of(
                new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY,
                        LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 5), 12),
                new MembershipRecord("Петро Коваль", MembershipKind.ANNUAL,
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1), 10)),
                MembershipStorage.load(export));
        assertTrue(output().contains("--- CSV EXPORT ---"), output());
        assertTrue(output().contains("Records in repository: 2"), output());
        assertTrue(output().contains("CSV records with header: 3"), output());
        assertTrue(output().contains("Restored records: 2"), output());
        assertTrue(output().contains("Header: клієнт,кінець,тип,початок,відвідування"), output());
        assertTrue(output().contains("Round-trip: true"), output());
        assertTrue(output().contains("Key \"Анна Литвин\" + 2026-10-05: restored, equal to the original"), output());
        assertTrue(output().contains("Export saved to " + export), output());
    }

    /* A storage error gives its own exit code, the message and the original cause. */
    @Test
    void exportErrorIsReportedWithCause(@TempDir Path folder) throws IOException {
        Path input = folder.resolve("input.csv");
        Files.write(input, List.of("Анна Литвин;Ранковий;1;12;600.00;2026-10-05"), StandardCharsets.UTF_8);
        Path blocker = Files.writeString(folder.resolve("blocker"), "not a folder", StandardCharsets.UTF_8);

        int exitCode = Main.run(new String[] {"--input", input.toString(),
            "--output", folder.resolve("report.txt").toString(),
            "--export", blocker.resolve("memberships.csv").toString()}, out, err);

        assertEquals(Main.EXIT_STORAGE_ERROR, exitCode);
        assertTrue(errors().startsWith("Error: cannot write CSV file"), errors());
        assertTrue(errors().contains("Cause: java.nio.file."), errors());
        assertTrue(output().contains("Report saved to"), "the report is written before the export");
    }

    /* An empty input file gives a zero report and "none" instead of empty lists. */
    @Test
    void emptyInputGivesEmptyReport(@TempDir Path folder) throws IOException {
        Path input = folder.resolve("empty.csv");
        Files.writeString(input, "", StandardCharsets.UTF_8);

        int exitCode = Main.run(new String[] {"--input", input.toString(),
            "--output", folder.resolve("report.txt").toString(),
            "--export", folder.resolve("memberships.csv").toString()}, out, err);

        assertEquals(Main.EXIT_OK, exitCode);
        assertTrue(output().contains("Valid records: 0"), output());
        assertTrue(output().contains("Active memberships: 0 (none)"), output());
        assertTrue(output().contains("Visit statistics: no data"), output());
        assertTrue(output().contains("Search \"Анна Литвин\": not found"), output());
        assertTrue(output().contains("Restored records: 0"), output());
        assertTrue(output().contains("Round-trip: true"), output());
        assertTrue(output().contains("Key \"Анна Литвин\": not found"), output());
    }

    /* A missing input file gives an error message and exit code 1 instead of a crash. */
    @Test
    void missingInputFileIsReported(@TempDir Path folder) {
        Path missing = folder.resolve("missing.csv");

        assertEquals(Main.EXIT_IO_ERROR, Main.run(new String[] {"--input", missing.toString()}, out, err));
        assertTrue(errors().startsWith("Error:"), errors());
    }

    private String output() {
        return outBuffer.toString(StandardCharsets.UTF_8);
    }

    private String errors() {
        return errBuffer.toString(StandardCharsets.UTF_8);
    }
}
