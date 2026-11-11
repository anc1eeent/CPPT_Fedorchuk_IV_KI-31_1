package ua.lpnu.kzp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Command-line entry point: reads gym memberships from CSV and prints the report of variant 20:
 * the indicators of labs 1–3 and the results of the Lab 4 stream queries.
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

    /** Client looked up by the search query of the report. */
    static final String SEARCH_CLIENT = "Анна Литвин";

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
            Repository<Membership> memberships = new Repository<>();
            memberships.addAll(readMemberships(options.input(), err));
            String report = formatReport(memberships.all());
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
     * Builds the report text: the indicators of Lab 1 and Lab 3, then the stream queries of Lab 4.
     *
     * @param memberships valid memberships
     * @return report text with platform line separators
     */
    static String formatReport(List<Membership> memberships) {
        // Lab 1 indicators of variant 20.
        MembershipSummary summary = MembershipSummary.from(memberships);

        // Lab 3 indicators: visitCost() is called through the base type, each subtype applies its own rule.
        double totalVisitCost = memberships.stream()
                .mapToDouble(Membership::visitCost)
                .sum(); 

        double maxVisitCost = memberships.stream()
                .mapToDouble(Membership::visitCost)
                .max()
                .orElse(0.0);
        
        // distinct() uses equals/hashCode: one entry per subtype and client.
        long uniqueClients = memberships.stream()
                .distinct()
                .count();

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
                uniqueClients,
                maxVisitCost,
                totalVisitCost)
                + formatQueries(memberships);
    }

    /**
     * Builds the report section with the results of the Lab 4 stream queries.
     *
     * @param memberships valid memberships
     * @return report section with platform line separators
     */
    static String formatQueries(List<Membership> memberships) {
        LocalDate date = MembershipReport.REPORT_DATE;

        List<Membership> active = MembershipReport.activeMemberships(memberships, date);
        String activeNames = active.stream()
                .map(Membership::getClient)
                .collect(Collectors.joining(", "));

        String clients = String.join(", ", MembershipReport.clientNames(memberships));

        String visitsByKind = MembershipReport.visitsByKind(memberships).entrySet().stream()
                .map(entry -> entry.getKey().label() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));

        // For an empty list min and max are Integer.MAX_VALUE and MIN_VALUE, so they are not shown.
        IntSummaryStatistics stats = MembershipReport.visitStatistics(memberships);
        String statistics = stats.getCount() == 0
                ? "no data"
                : String.format(Locale.ROOT, "count=%d, sum=%d, min=%d, max=%d, average=%.2f",
                        stats.getCount(), stats.getSum(), stats.getMin(), stats.getMax(), stats.getAverage());

        String top = MembershipReport.topByVisits(memberships, MembershipReport.TOP_SIZE).stream()
                .map(membership -> membership.getClient() + " (" + membership.getVisits() + ")")
                .collect(Collectors.joining(", "));

        // Optional.map runs only when a membership was found; otherwise orElse gives the text.
        String search = MembershipReport.findByClient(memberships, SEARCH_CLIENT)
                .map(membership -> String.format(Locale.ROOT, "found, %d visits, %s to %s",
                        membership.getVisits(), membership.getStartDate(), membership.getEndDate()))
                .orElse("not found");

        return String.format(Locale.ROOT,
                "%n--- STREAM QUERIES (report date %s) ---%n"
                        + "Active memberships: %d (%s)%n"
                        + "Clients: %s%n"
                        + "Visits by kind: %s%n"
                        + "Visit statistics: %s%n"
                        + "Top %d by visits: %s%n"
                        + "Search \"%s\": %s%n",
                date, active.size(), orNone(activeNames), orNone(clients), orNone(visitsByKind), statistics,
                MembershipReport.TOP_SIZE, orNone(top), SEARCH_CLIENT, search);
    }

    /* Shows "none" instead of an empty list in the report. */
    private static String orNone(String joined) {
        return joined.isEmpty() ? "none" : joined;
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
