package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Writes objects of any class to CSV using reflection.
 *
 * <p>The exporter knows nothing about the gym domain. It takes the class of the records, finds its
 * instance fields marked with {@link CsvColumn} (including fields inherited from superclasses),
 * orders them by Java field name and uses the same order for the header and for every row.
 * Values are turned into text with {@link String#valueOf(Object)}; {@code null} becomes an empty
 * field.</p>
 *
 * <p>Format: the first line is the header, fields are separated by commas, each record ends with
 * {@link System#lineSeparator()}. A value that contains a comma, a double quote, a carriage return
 * or a line feed is enclosed in double quotes, and every double quote inside it is doubled.</p>
 */
public final class CsvExporter {

    /** Separator between the fields of one record. */
    public static final char SEPARATOR = ',';

    /** Character that encloses a field with special characters. */
    public static final char QUOTE = '"';

    private CsvExporter() {
    }

    /**
     * Returns the column names of a class in export order.
     *
     * @param type class with fields marked by {@link CsvColumn}
     * @return column names from the annotations, ordered by Java field name
     * @throws DataStorageException if the class has no annotated fields, a column name is blank or a
     *                              field cannot be made accessible
     * @throws NullPointerException if {@code type} is null
     */
    public static List<String> header(Class<?> type) throws DataStorageException {
        return annotatedFields(type).stream()
                .map(field -> field.getAnnotation(CsvColumn.class).value())
                .toList();
    }

    /**
     * Builds the CSV text of the records: the header line and one line per record.
     *
     * <p>The class is passed separately because it cannot be taken from an empty list, and the
     * header must be written even when there are no records.</p>
     *
     * @param <T>     type of the records
     * @param type    class of the records
     * @param records records to export, in this order
     * @return CSV text; for an empty list only the header line
     * @throws DataStorageException if the class has no annotated fields or a field cannot be read
     * @throws NullPointerException if an argument or a record is null
     */
    public static <T> String toCsv(Class<T> type, List<? extends T> records) throws DataStorageException {
        Objects.requireNonNull(records, "records must not be null");
        List<Field> fields = annotatedFields(type);

        // One list of fields gives both the header and the values, so the columns always match.
        StringBuilder csv = new StringBuilder();
        csv.append(fields.stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(String.valueOf(SEPARATOR))));
        csv.append(System.lineSeparator());

        for (T item : records) {
            Objects.requireNonNull(item, "records must not contain null");
            List<String> values = new ArrayList<>(fields.size());
            for (Field field : fields) {
                values.add(escape(readField(field, item)));
            }
            csv.append(String.join(String.valueOf(SEPARATOR), values));
            csv.append(System.lineSeparator());
        }
        return csv.toString();
    }

    /**
     * Writes the records to a UTF-8 CSV file.
     *
     * <p>Missing parent folders are created. An existing file is replaced completely
     * ({@link StandardOpenOption#TRUNCATE_EXISTING}), so no old lines remain after a shorter export.</p>
     *
     * @param <T>     type of the records
     * @param path    CSV file to write
     * @param type    class of the records
     * @param records records to export, in this order
     * @throws DataStorageException if the class cannot be exported, or the file cannot be written
     *                              (the {@link IOException} is the cause)
     * @throws NullPointerException if an argument or a record is null
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(path, "path must not be null");
        String csv = toCsv(type, records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, csv, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new DataStorageException("cannot write CSV file " + path, e);
        }
    }

    /**
     * Turns a value into one CSV field.
     *
     * @param value value of a field, may be null
     * @return the text of the value; enclosed in quotes with doubled inner quotes if it contains a
     *         comma, a quote or a line break; empty for {@code null}
     */
    static String escape(Object value) {
        String text = Objects.toString(value, "");
        boolean special = text.indexOf(SEPARATOR) >= 0
                || text.indexOf(QUOTE) >= 0
                || text.indexOf('\r') >= 0
                || text.indexOf('\n') >= 0;
        if (!special) {
            return text;
        }
        String quote = String.valueOf(QUOTE);
        return quote + text.replace(quote, quote + quote) + quote;
    }

    /*
     * Collects the annotated instance fields of the class and of all its superclasses up to Object,
     * sorted by field name, and makes them readable. getDeclaredFields() also returns private fields,
     * but only of one class, so the loop walks up the chain. Static fields are not data of a record
     * and are skipped.
     */
    private static List<Field> annotatedFields(Class<?> type) throws DataStorageException {
        Objects.requireNonNull(type, "type must not be null");
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(CsvColumn.class) && !Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
        }
        if (fields.isEmpty()) {
            throw new DataStorageException("class " + type.getName() + " has no fields marked with @"
                    + CsvColumn.class.getSimpleName());
        }
        fields.sort(Comparator.comparing(Field::getName));

        for (Field field : fields) {
            if (field.getAnnotation(CsvColumn.class).value().isBlank()) {
                throw new DataStorageException("field " + describe(field) + " has a blank CSV column name");
            }
            // Private fields need this; it returns false instead of throwing if a module forbids access.
            if (!field.trySetAccessible()) {
                throw new DataStorageException("no access to field " + describe(field));
            }
        }
        return fields;
    }

    /* Reads one field of a record; an access error becomes the cause. */
    private static Object readField(Field field, Object item) throws DataStorageException {
        try {
            return field.get(item);
        } catch (IllegalAccessException e) {
            throw new DataStorageException("cannot read field " + describe(field), e);
        }
    }

    /* Names a field together with its class for error messages. */
    private static String describe(Field field) {
        return field.getDeclaringClass().getSimpleName() + "." + field.getName();
    }
}
