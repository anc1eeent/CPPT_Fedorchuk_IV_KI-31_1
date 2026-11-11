package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks the sample file {@code data/input.csv} that is shipped with the project. */
class SampleDataTest {

    /* The sample has seven valid rows and one row for every kind of invalid data. */
    @Test
    void sampleContainsEveryInvalidRowType() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream warnings = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        List<Membership> memberships = Main.readMemberships(CommandLine.DEFAULT_INPUT, warnings);

        assertEquals(7, memberships.size());
        assertEquals(List.of(
                "Skipped line 3: months is not a whole number: 'помилка'",
                "Skipped line 5: plan must not be blank",
                "Skipped line 6: expected 6 fields, got 1",
                "Skipped line 7: visits must be at least 1, got -5",
                "Skipped line 8: expected 6 fields, got 5",
                "Skipped line 11: startDate is not a date (yyyy-mm-dd): '2026-13-01'"),
                buffer.toString(StandardCharsets.UTF_8).lines().toList());
    }

    /* Indicators of the valid rows: 123 visits over 7 memberships, revenue 5450.50, longest 12 months. */
    @Test
    void sampleGivesExpectedIndicators() throws IOException {
        PrintStream ignored = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        List<Membership> memberships = Main.readMemberships(CommandLine.DEFAULT_INPUT, ignored);

        assertEquals(new MembershipSummary(7, 123.0 / 7, 5450.50, 12), MembershipSummary.from(memberships));
    }
}
