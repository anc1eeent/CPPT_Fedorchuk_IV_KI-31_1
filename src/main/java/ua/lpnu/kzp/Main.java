package ua.lpnu.kzp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Command-line entry point: reads gym memberships from CSV and prints the report of variant 20.
 *
 * <p>Exit codes: {@value #EXIT_OK} on success, {@value #EXIT_IO_ERROR} if a file cannot be read
 * or written, {@value #EXIT_USAGE} for invalid options.</p>
 */
public final class Main {

    /** Exit code for a successful run. */
    static final int EXIT_OK = 0;

    /** Exit code when the input cannot be read or the report cannot be written. */
    static final int EXIT_IO_ERROR = 1;

    /** Exit code for an unknown option or an option without its value. */
    static final int EXIT_USAGE = 2;

    private Main() {
    }

    /**
     * Starts the program.
     *
     * @param args command-line options, see {@link CommandLine#USAGE}
     */
    public static void main(String[] args) {
        // Explicit UTF-8, so Ukrainian text is not replaced with '?' on Windows consoles.
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);
        int exitCode = run(args, out, err);
        if (exitCode != EXIT_OK) {
            System.exit(exitCode);
        }
    }

    /**
     * Runs the program with the given streams, so tests can capture the output.
     *
     * @param args command-line options
     * @param out  stream for the report and normal messages
     * @param err  stream for skipped lines and errors
     * @return exit code
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        CommandLine options;
        try {
            options = CommandLine.parse(args);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.println();
            err.print(CommandLine.USAGE);
            return EXIT_USAGE;
        }
        if (options.help()) {
            out.print(CommandLine.USAGE);
            return EXIT_OK;
        }
        if (options.version()) {
            out.println("Gym Membership Report " + appVersion());
            return EXIT_OK;
        }

        try {
            List<Membership> memberships = readMemberships(options.input(), err);
            String report = formatReport(memberships);
            out.print(report);
            writeReport(options.output(), report);
            out.println("Report saved to " + options.output());
            return EXIT_OK;
        } catch (IOException e) {
            err.println("Error: cannot access file " + e.getMessage());
            return EXIT_IO_ERROR;
        }
    }

    /**
     * Returns the application version from the JAR manifest.
     *
     * @return the version, or "dev" when the program runs outside the JAR
     */
    static String appVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }

    /**
     * Reads memberships from a UTF-8 CSV file. Invalid lines are skipped and reported.
     *
     * @param input    CSV file, one membership per line
     * @param warnings stream that receives one message per skipped line
     * @return valid memberships in file order
     * @throws IOException if the file cannot be read
     */
    static List<Membership> readMemberships(Path input, PrintStream warnings) throws IOException {
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        List<Membership> memberships = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            try {
                memberships.add(Membership.fromCsv(lines.get(i)));
            } catch (IllegalArgumentException e) {
                warnings.printf(Locale.ROOT, "Skipped line %d: %s%n", i + 1, e.getMessage());
            }
        }
        return memberships;
    }

    /**
     * Builds the report text with the indicators of Lab 1 and Lab 3.
     *
     * @param memberships valid memberships
     * @return report text with platform line separators
     */
    static String formatReport(List<Membership> memberships) {
        // Lab 1 indicators of variant 20.
        MembershipSummary summary = MembershipSummary.from(memberships);

        // Lab 3 indicators: visitCost() is called through the base type, each subtype applies its own rule.
        double totalVisitCost = 0.0;
        double maxVisitCost = 0.0;
        for (Membership membership : memberships) {
            double cost = membership.visitCost();
            totalVisitCost += cost;
            maxVisitCost = Math.max(maxVisitCost, cost);
        }
        // equals/hashCode keep one entry per subtype and client.
        Set<Membership> uniqueMemberships = new HashSet<>(memberships);

        return String.format(Locale.ROOT,
                "--- REPORT ---%n"
                        + "Valid records: %d%n"
                        + "Total revenue: %.2f UAH%n"
                        + "Longest membership: %d months%n"
                        + "Average visits: %.2f%n"
                        + "Unique clients: %d%n"
                        + "Highest visit cost: %.2f UAH%n"
                        + "Total visit cost: %.2f UAH%n",
                summary.validCount(),
                summary.totalRevenue(),
                summary.maxMonths(),
                summary.averageVisits(),
                uniqueMemberships.size(),
                maxVisitCost,
                totalVisitCost);
    }

    /**
     * Writes the report as UTF-8, creating missing parent folders.
     *
     * @param output report file
     * @param report report text
     * @throws IOException if the file cannot be written
     */
    static void writeReport(Path output, String report) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}
