package ua.lpnu.kzp;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Options given to the program on the command line.
 *
 * @param input   CSV file to read
 * @param output  report file to write
 * @param export  CSV file to export the membership records to
 * @param help    {@code true} if the usage text was requested
 * @param version {@code true} if the version was requested
 */
public record CommandLine(Path input, Path output, Path export, boolean help, boolean version) {

    /** CSV file read when {@code --input} is not given. */
    public static final Path DEFAULT_INPUT = Path.of("data", "input.csv");

    /** Report file written when {@code --output} is not given. */
    public static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    /** CSV file of the membership records written when {@code --export} is not given. */
    public static final Path DEFAULT_EXPORT = Path.of("out", "memberships.csv");

    /** Usage text printed by {@code --help} and after an invalid option. */
    public static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -jar gym-membership.jar [options]",
            "",
            "Reads gym memberships from a CSV file and prints a report.",
            "",
            "Options:",
            "  --input <path>    CSV file to read (default: data/input.csv)",
            "  --output <path>   report file to write (default: out/report.txt)",
            "  --export <path>   CSV file to export the records to (default: out/memberships.csv)",
            "  --version         print the version and exit",
            "  --help            print this help and exit",
            "");

    /**
     * Checks that all paths are present.
     *
     * @throws NullPointerException if a path is null
     */
    public CommandLine {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(export, "export must not be null");
    }

    /**
     * Parses the command-line arguments.
     *
     * @param args arguments as passed to {@code main}
     * @return parsed options; paths that were not given keep their defaults
     * @throws IllegalArgumentException if an option is unknown or has no value
     */
    public static CommandLine parse(String... args) {
        Objects.requireNonNull(args, "args must not be null");
        Path input = DEFAULT_INPUT;
        Path output = DEFAULT_OUTPUT;
        Path export = DEFAULT_EXPORT;
        boolean help = false;
        boolean version = false;
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            switch (option) {
                case "--help", "-h" -> help = true;
                case "--version" -> version = true;
                case "--input" -> {
                    i++;
                    input = Path.of(valueOf(option, args, i));
                }
                case "--output" -> {
                    i++;
                    output = Path.of(valueOf(option, args, i));
                }
                case "--export" -> {
                    i++;
                    export = Path.of(valueOf(option, args, i));
                }
                default -> throw new IllegalArgumentException("unknown option: " + option);
            }
        }
        return new CommandLine(input, output, export, help, version);
    }

    /* Returns the value that follows an option, or fails if there is none. */
    private static String valueOf(String option, String[] args, int index) {
        if (index >= args.length || args[index].startsWith("--") || args[index].isBlank()) {
            throw new IllegalArgumentException(option + " needs a path");
        }
        return args[index];
    }
}
