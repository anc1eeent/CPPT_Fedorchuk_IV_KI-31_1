package ua.lpnu.kzp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Saves membership records to CSV and reads them back.
 *
 * <p>Writing is done by the universal {@link CsvExporter}; reading uses {@link CsvParser} for the
 * CSV structure and {@link MembershipRecord#fromFields(List)} for the values, because only the
 * domain class knows how to parse its dates, enum and numbers.</p>
 *
 * <p>Contract for empty data: an empty file and a file with only the header are both valid and
 * give an empty list. A file whose first record is not the expected header is rejected.</p>
 */
public final class MembershipStorage {

    private MembershipStorage() {
    }

    /**
     * Writes the records to a UTF-8 CSV file with a header line.
     *
     * @param path    CSV file to write; missing folders are created, an old file is replaced
     * @param records records to save, in this order
     * @throws DataStorageException if the file cannot be written
     * @throws NullPointerException if an argument or a record is null
     */
    public static void save(Path path, List<MembershipRecord> records) throws DataStorageException {
        CsvExporter.write(path, MembershipRecord.class, records);
    }

    /**
     * Reads the records from a UTF-8 CSV file written by {@link #save(Path, List)}.
     *
     * @param path CSV file to read
     * @return records in file order; empty for an empty file or a file with only the header
     * @throws DataStorageException if the file cannot be read, the CSV is broken, the header is
     *                              different, or a record is invalid; the message names the file and
     *                              the record number, the original error is the cause
     * @throws NullPointerException if {@code path} is null
     */
    public static List<MembershipRecord> load(Path path) throws DataStorageException {
        List<List<String>> rows = CsvParser.read(path);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> expectedHeader = CsvExporter.header(MembershipRecord.class);
        if (!rows.get(0).equals(expectedHeader)) {
            throw new DataStorageException("unexpected header in " + path + ": expected "
                    + String.join(",", expectedHeader) + ", got " + String.join(",", rows.get(0)));
        }

        List<MembershipRecord> records = new ArrayList<>(rows.size() - 1);
        for (int i = 1; i < rows.size(); i++) {
            try {
                records.add(MembershipRecord.fromFields(rows.get(i)));
            } catch (DataStorageException e) {
                // Record numbers count data records only: the header is record 0.
                throw new DataStorageException(
                        "cannot read record " + i + " of " + path + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(records);
    }

    /**
     * Saves the records, reads them back and compares the two lists.
     *
     * @param path    CSV file to write and read
     * @param records records to save
     * @return result of the round-trip
     * @throws DataStorageException if the file cannot be written or read back
     * @throws NullPointerException if an argument or a record is null
     */
    public static RoundTrip roundTrip(Path path, List<MembershipRecord> records) throws DataStorageException {
        Objects.requireNonNull(records, "records must not be null");
        save(path, records);
        List<MembershipRecord> restored = load(path);
        return new RoundTrip(List.copyOf(records), restored, CsvParser.read(path).size());
    }

    /**
     * Result of writing records to CSV and reading them back.
     *
     * @param original   records that were saved
     * @param restored   records that were read back
     * @param csvRecords number of CSV records in the file, the header included
     */
    public record RoundTrip(List<MembershipRecord> original, List<MembershipRecord> restored, int csvRecords) {

        /**
         * Copies both lists, so the result cannot be changed from outside.
         *
         * @throws NullPointerException if a list or one of its records is null
         */
        public RoundTrip {
            original = List.copyOf(original);
            restored = List.copyOf(restored);
        }

        /**
         * Checks that the restored records are equal to the original ones, in the same order.
         *
         * @return {@code true} if both lists are equal by {@code equals}
         */
        public boolean isEqual() {
            return original.equals(restored);
        }
    }
}
