package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** Tests for parsing command-line options. */
class CommandLineTest {

    /* Without options the program reads and writes the default files. */
    @Test
    void usesDefaultsWithoutOptions() {
        CommandLine options = CommandLine.parse();

        assertEquals(CommandLine.DEFAULT_INPUT, options.input());
        assertEquals(CommandLine.DEFAULT_OUTPUT, options.output());
        assertEquals(CommandLine.DEFAULT_EXPORT, options.export());
        assertFalse(options.help());
        assertFalse(options.version());
    }

    /* --input, --output and --export replace the defaults in any order. */
    @Test
    void readsPaths() {
        CommandLine options = CommandLine.parse(
                "--export", "records.csv", "--output", "result.txt", "--input", "members.csv");

        assertEquals(Path.of("members.csv"), options.input());
        assertEquals(Path.of("result.txt"), options.output());
        assertEquals(Path.of("records.csv"), options.export());
    }

    /* --help, -h and --version are flags without values. */
    @Test
    void readsFlags() {
        assertTrue(CommandLine.parse("--help").help());
        assertTrue(CommandLine.parse("-h").help());
        assertTrue(CommandLine.parse("--version").version());
    }

    /* An unknown option is rejected with its name in the message. */
    @Test
    void rejectsUnknownOption() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CommandLine.parse("--colour"));

        assertTrue(error.getMessage().contains("--colour"), error.getMessage());
    }

    /* An option that needs a path fails when the path is missing. */
    @Test
    void rejectsOptionWithoutValue() {
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("--input"));
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("--input", "--output", "x"));
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("--output", " "));
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("--export"));
    }
}
