package ua.lpnu.kzp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field that is exported to CSV and holds the name of its column.
 *
 * <p>The annotation only describes the field; the export itself is done by {@link CsvExporter}.
 * {@link RetentionPolicy#RUNTIME} keeps the annotation in the class file and makes it visible to
 * reflection while the program runs; {@link ElementType#FIELD} allows it only on fields. On a
 * record component the annotation is copied to the private field of the record.</p>
 *
 * <p>The column name sets the header text, not the column order: the exporter orders the columns
 * by the Java field names.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {

    /**
     * Returns the CSV column header of the field.
     *
     * @return column name, not blank
     */
    String value();
}
