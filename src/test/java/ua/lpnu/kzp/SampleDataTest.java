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

    /* The sample has three valid rows and one row for every kind of invalid data. */
    @Test
    void sampleContainsEveryInvalidRowType() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream warnings = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        List<Membership> memberships = Main.readMemberships(CommandLine.DEFAULT_INPUT, warnings);

        assertEquals(3, memberships.size());
        assertEquals(List.of(
                "Skipped line 3: months is not a whole number: 'помилка'",
                "Skipped line 5: plan must not be blank",
                "Skipped line 6: expected 5 fields, got 1",
                "Skipped line 7: visits must be at least 1, got -5",
                "Skipped line 8: expected 5 fields, got 4"),
                buffer.toString(StandardCharsets.UTF_8).lines().toList());
    }

    /* Invalid rows do not change the indicators of the valid ones. */
    @Test
    void sampleGivesLab1Indicators() throws IOException {
        PrintStream ignored = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        List<Membership> memberships = Main.readMemberships(CommandLine.DEFAULT_INPUT, ignored);

        assertEquals(new MembershipSummary(3, 25.0, 2900.50, 12), MembershipSummary.from(memberships));
    }
}
