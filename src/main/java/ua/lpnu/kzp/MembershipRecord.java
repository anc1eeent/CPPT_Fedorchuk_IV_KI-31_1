package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Flat membership record of variant 20 that is exported to CSV and read back.
 *
 * <p>Every component is marked with {@link CsvColumn}, so {@link CsvExporter} finds it by reflection.
 * The exporter orders the columns by the Java names of the fields:</p>
 *
 * <pre>
 * customer, endDate, kind,  startDate, visits
 * клієнт,   кінець,  тип,   початок,   відвідування
 * </pre>
 *
 * <p>As a record it is immutable, and {@code equals}/{@code hashCode} compare all five fields,
 * so a restored list can be compared with the original by {@code equals}. A membership is
 * identified by {@link #key()}: customer and start date.</p>
 *
 * @param customer  client full name, not blank
 * @param kind      membership category, not null
 * @param startDate first day of the membership, not null
 * @param endDate   first day after the membership, later than {@code startDate}
 * @param visits    number of visits, at least 1
 */
public record MembershipRecord(
        @CsvColumn(MembershipRecord.CUSTOMER_COLUMN) String customer,
        @CsvColumn(MembershipRecord.KIND_COLUMN) MembershipKind kind,
        @CsvColumn(MembershipRecord.START_DATE_COLUMN) LocalDate startDate,
        @CsvColumn(MembershipRecord.END_DATE_COLUMN) LocalDate endDate,
        @CsvColumn(MembershipRecord.VISITS_COLUMN) int visits) {

    /** CSV column of {@link #customer()}. */
    public static final String CUSTOMER_COLUMN = "клієнт";

    /** CSV column of {@link #kind()}. */
    public static final String KIND_COLUMN = "тип";

    /** CSV column of {@link #startDate()}. */
    public static final String START_DATE_COLUMN = "початок";

    /** CSV column of {@link #endDate()}. */
    public static final String END_DATE_COLUMN = "кінець";

    /** CSV column of {@link #visits()}. */
    public static final String VISITS_COLUMN = "відвідування";

    /** Number of CSV fields in one record. */
    public static final int FIELD_COUNT = 5;

    // Positions of the fields in a CSV record. They follow the exporter order: Java field names sorted
    // alphabetically (customer, endDate, kind, startDate, visits). MembershipStorage checks the header,
    // so a file with a different column order is rejected before these positions are used.
    private static final int CUSTOMER = 0;
    private static final int END_DATE = 1;
    private static final int KIND = 2;
    private static final int START_DATE = 3;
    private static final int VISITS = 4;

    /**
     * Checks the invariants of the record.
     *
     * @throws IllegalArgumentException if the customer is blank, the end date is not after the start
     *                                  date or visits are less than 1
     * @throws NullPointerException     if the customer, kind or a date is null
     */
    public MembershipRecord {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (customer.isBlank()) {
            throw new IllegalArgumentException("customer must not be blank");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "endDate must be after startDate, got " + startDate + " to " + endDate);
        }
        if (visits < 1) {
            throw new IllegalArgumentException("visits must be at least 1, got " + visits);
        }
    }

    /**
     * Builds the export record of a membership.
     *
     * @param membership membership of the model
     * @return record with the client, kind, start date, end date and visits of the membership
     * @throws NullPointerException if {@code membership} is null
     */
    public static MembershipRecord from(Membership membership) {
        Objects.requireNonNull(membership, "membership must not be null");
        return new MembershipRecord(membership.getClient(), membership.getKind(),
                membership.getStartDate(), membership.getEndDate(), membership.getVisits());
    }

    /**
     * Restores a record from the fields of one CSV record, in the column order of the exporter.
     *
     * @param fields text values: customer, end date, kind, start date, visits
     * @return restored record
     * @throws DataStorageException if the field count is wrong, a value cannot be parsed (the parse
     *                              error is the cause) or the record breaks an invariant (the
     *                              {@link IllegalArgumentException} is the cause)
     * @throws NullPointerException if {@code fields} is null
     */
    public static MembershipRecord fromFields(List<String> fields) throws DataStorageException {
        Objects.requireNonNull(fields, "fields must not be null");
        if (fields.size() != FIELD_COUNT) {
            throw new DataStorageException("expected " + FIELD_COUNT + " fields, got " + fields.size());
        }
        String customer = fields.get(CUSTOMER);
        LocalDate endDate = parseDate(fields.get(END_DATE), END_DATE_COLUMN);
        MembershipKind kind = parseKind(fields.get(KIND));
        LocalDate startDate = parseDate(fields.get(START_DATE), START_DATE_COLUMN);
        int visits = parseNumber(fields.get(VISITS), VISITS_COLUMN);
        try {
            return new MembershipRecord(customer, kind, startDate, endDate, visits);
        } catch (IllegalArgumentException e) {
            throw new DataStorageException("invalid membership record: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the key that identifies the membership in the round-trip check of variant 20.
     *
     * @return customer and start date
     */
    public Key key() {
        return new Key(customer, startDate);
    }

    /**
     * Key of a membership record: one client can have several memberships, but not two that
     * start on the same day.
     *
     * @param customer  client full name
     * @param startDate first day of the membership
     */
    public record Key(String customer, LocalDate startDate) {

        /**
         * Checks that both parts of the key are present.
         *
         * @throws NullPointerException if a part is null
         */
        public Key {
            Objects.requireNonNull(customer, "customer must not be null");
            Objects.requireNonNull(startDate, "startDate must not be null");
        }
    }

    /* Parses a whole number; the NumberFormatException becomes the cause. */
    private static int parseNumber(String text, String column) throws DataStorageException {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new DataStorageException("field '" + column + "' is not a whole number: '" + text + "'", e);
        }
    }

    /* Parses an ISO date; the DateTimeParseException becomes the cause. */
    private static LocalDate parseDate(String text, String column) throws DataStorageException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new DataStorageException(
                    "field '" + column + "' is not a date (yyyy-mm-dd): '" + text + "'", e);
        }
    }

    /* Parses an enum constant name; the IllegalArgumentException of valueOf becomes the cause. */
    private static MembershipKind parseKind(String text) throws DataStorageException {
        try {
            return MembershipKind.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new DataStorageException("field '" + KIND_COLUMN + "' is not one of "
                    + Arrays.toString(MembershipKind.values()) + ": '" + text + "'", e);
        }
    }
}
