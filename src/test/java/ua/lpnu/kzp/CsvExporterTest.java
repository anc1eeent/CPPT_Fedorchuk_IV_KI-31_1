package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for the reflective CSV exporter. */
class CsvExporterTest {

    private static final String NL = System.lineSeparator();

    private static final MembershipRecord ANNA = new MembershipRecord("Анна Литвин", MembershipKind.MONTHLY,
            LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 5), 12);
    private static final MembershipRecord PETRO = new MembershipRecord("Коваль, Петро \"Ранковий\"",
            MembershipKind.ANNUAL, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1), 10);

    /* A class of a different domain: the exporter must not depend on the gym classes. */
    static final class Book {
        @CsvColumn("назва")
        private final String title;

        @CsvColumn("автор")
        private final String author;

        @CsvColumn("ціна")
        private final double price;

        /* Not annotated, so not exported. */
        @SuppressWarnings("unused")
        private final String note = "internal";

        Book(String title, String author, double price) {
            this.title = title;
            this.author = author;
            this.price = price;
        }
    }

    /* Superclass with an annotated field and an annotated static field. */
    static class Base {
        @CsvColumn("службове")
        static final String VERSION = "1";

        @CsvColumn("код")
        private final String code;

        Base(String code) {
            this.code = code;
        }
    }

    /* Subclass: its own field and the inherited one are exported together. */
    static final class Derived extends Base {
        @CsvColumn("назва")
        private final String name;

        Derived(String code, String name) {
            super(code);
            this.name = name;
        }
    }

    /* A class without annotated fields cannot be exported. */
    static final class Plain {
        @SuppressWarnings("unused")
        private final String value = "x";
    }

    /* A class with a blank column name is a programming error of the class. */
    static final class BlankColumn {
        @CsvColumn(" ")
        @SuppressWarnings("unused")
        private final String value = "x";
    }

    /* Header from the annotations, then one line per record in the same column order. */
    @Test
    void writesHeaderAndRecords() throws DataStorageException {
        String csv = CsvExporter.toCsv(MembershipRecord.class, List.of(ANNA));

        assertEquals("клієнт,кінець,тип,початок,відвідування" + NL
                + "Анна Литвин,2026-11-05,MONTHLY,2026-10-05,12" + NL, csv);
    }

    /* Comma, quote and line break make a field quoted; inner quotes are doubled. */
    @Test
    void escapesSpecialCharacters() {
        assertEquals("Анна Литвин", CsvExporter.escape("Анна Литвин"));
        assertEquals("\"Коваль, Петро\"", CsvExporter.escape("Коваль, Петро"));
        assertEquals("\"Олена \"\"Спорт\"\"\"", CsvExporter.escape("Олена \"Спорт\""));
        assertEquals("\"рядок 1\nрядок 2\"", CsvExporter.escape("рядок 1\nрядок 2"));
        assertEquals("\"рядок 1\r\nрядок 2\"", CsvExporter.escape("рядок 1\r\nрядок 2"));
        assertEquals("", CsvExporter.escape(null));
        assertEquals("12", CsvExporter.escape(12));
    }

    /* A record with a comma and quotes in the customer gives one escaped field. */
    @Test
    void escapesValuesOfRecords() throws DataStorageException {
        String csv = CsvExporter.toCsv(MembershipRecord.class, List.of(PETRO));

        assertTrue(csv.endsWith(NL + "\"Коваль, Петро \"\"Ранковий\"\"\",2026-11-01,ANNUAL,2026-08-01,10" + NL), csv);
    }

    /* The class gives the header even when there is no record. */
    @Test
    void emptyListGivesHeaderOnly() throws DataStorageException {
        assertEquals("клієнт,кінець,тип,початок,відвідування" + NL,
                CsvExporter.toCsv(MembershipRecord.class, List.of()));
    }

    /* The same exporter works for an unrelated class; columns follow sorted field names. */
    @Test
    void exportsUnrelatedClass() throws DataStorageException {
        List<Book> books = List.of(
                new Book("Тіні забутих предків", "Коцюбинський, М.", 250.0),
                new Book("Кайдашева сім'я", null, 199.5));

        String csv = CsvExporter.toCsv(Book.class, books);

        assertEquals(List.of("автор", "ціна", "назва"), CsvExporter.header(Book.class));
        assertEquals("автор,ціна,назва" + NL
                + "\"Коцюбинський, М.\",250.0,Тіні забутих предків" + NL
                + ",199.5,Кайдашева сім'я" + NL, csv);
    }

    /* Inherited fields are found, static fields are not data and are skipped. */
    @Test
    void includesInheritedFieldsAndSkipsStatic() throws DataStorageException {
        String csv = CsvExporter.toCsv(Derived.class, List.of(new Derived("D-1", "Гантелі")));

        assertEquals("код,назва" + NL + "D-1,Гантелі" + NL, csv);
    }

    /* Reflection error: no annotated fields. */
    @Test
    void rejectsClassWithoutAnnotatedFields() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> CsvExporter.toCsv(Plain.class, List.of(new Plain())));

        assertTrue(error.getMessage().contains("Plain has no fields marked with @CsvColumn"), error.getMessage());
    }

    /* Reflection error: a blank column name. */
    @Test
    void rejectsBlankColumnName() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> CsvExporter.header(BlankColumn.class));

        assertTrue(error.getMessage().contains("BlankColumn.value has a blank CSV column name"), error.getMessage());
    }

    /* null is neither a class nor a record. */
    @Test
    void rejectsNullArguments() {
        List<MembershipRecord> withNull = new ArrayList<>(Arrays.asList(ANNA, null));

        assertThrows(NullPointerException.class, () -> CsvExporter.toCsv(null, List.of()));
        assertThrows(NullPointerException.class, () -> CsvExporter.toCsv(MembershipRecord.class, null));
        assertThrows(NullPointerException.class, () -> CsvExporter.toCsv(MembershipRecord.class, withNull));
    }

    /* The file is UTF-8 and missing folders are created. */
    @Test
    void writesUtf8FileAndCreatesFolders(@TempDir Path folder) throws DataStorageException, IOException {
        Path file = folder.resolve("nested").resolve("memberships.csv");

        CsvExporter.write(file, MembershipRecord.class, List.of(ANNA, PETRO));

        String expected = CsvExporter.toCsv(MembershipRecord.class, List.of(ANNA, PETRO));
        assertTrue(Arrays.equals(expected.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file)),
                "file bytes are not the UTF-8 encoding of the CSV text");
    }

    /* A shorter export replaces the old file completely. */
    @Test
    void replacesOldFile(@TempDir Path folder) throws DataStorageException, IOException {
        Path file = folder.resolve("memberships.csv");
        CsvExporter.write(file, MembershipRecord.class, List.of(ANNA, PETRO, ANNA));

        CsvExporter.write(file, MembershipRecord.class, List.of(PETRO));

        assertEquals(CsvExporter.toCsv(MembershipRecord.class, List.of(PETRO)),
                Files.readString(file, StandardCharsets.UTF_8));
    }

    /* File error: the parent "folder" is a file, so the IOException becomes the cause. */
    @Test
    void reportsFileErrorWithCause(@TempDir Path folder) throws IOException {
        Path blocker = Files.writeString(folder.resolve("blocker"), "not a folder", StandardCharsets.UTF_8);
        Path file = blocker.resolve("memberships.csv");

        DataStorageException error = assertThrows(DataStorageException.class,
                () -> CsvExporter.write(file, MembershipRecord.class, List.of(ANNA)));

        assertEquals("cannot write CSV file " + file, error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
    }
}
