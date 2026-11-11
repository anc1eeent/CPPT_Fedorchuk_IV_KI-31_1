package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for the CSV document parser. */
class CsvParserTest {

    /* Plain fields: commas separate fields, line breaks separate records. */
    @Test
    void splitsRecordsAndFields() throws DataStorageException {
        assertEquals(List.of(List.of("клієнт", "тип"), List.of("Анна Литвин", "MONTHLY")),
                CsvParser.parseDocument("клієнт,тип\nАнна Литвин,MONTHLY\n"));
    }

    /* Inside quotes a comma and a line break are data, and "" is one quote. */
    @Test
    void readsQuotedFieldWithCommaQuoteAndLineBreak() throws DataStorageException {
        String csv = "\"Коваль, Петро \"\"Ранковий\"\"\nкорпоративний клієнт\",10\n";

        assertEquals(List.of(List.of("Коваль, Петро \"Ранковий\"\nкорпоративний клієнт", "10")),
                CsvParser.parseDocument(csv));
    }

    /* Windows (\r\n) and Unix (\n) line breaks give the same records. */
    @Test
    void acceptsBothLineSeparators() throws DataStorageException {
        List<List<String>> unix = CsvParser.parseDocument("a,b\nc,d\n");
        List<List<String>> windows = CsvParser.parseDocument("a,b\r\nc,d\r\n");

        assertEquals(unix, windows);
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), windows);
    }

    /* The last record does not need a line break; an empty last field is kept. */
    @Test
    void readsLastRecordWithoutLineBreak() throws DataStorageException {
        assertEquals(List.of(List.of("a", "b")), CsvParser.parseDocument("a,b"));
        assertEquals(List.of(List.of("a", "")), CsvParser.parseDocument("a,"));
        assertEquals(List.of(List.of("", "x")), CsvParser.parseDocument("\"\",x"));
    }

    /* Empty text has no records; a header without data is one record. */
    @Test
    void handlesEmptyDocumentAndHeaderOnly() throws DataStorageException {
        assertEquals(List.of(), CsvParser.parseDocument(""));
        assertEquals(List.of(List.of("клієнт", "тип")), CsvParser.parseDocument("клієнт,тип\n"));
    }

    /* A quote in the middle of an unquoted field is an ordinary character. */
    @Test
    void keepsQuoteInsideUnquotedField() throws DataStorageException {
        assertEquals(List.of(List.of("зал \"А\"", "1")), CsvParser.parseDocument("зал \"А\",1"));
    }

    /* Every value escaped by the exporter is parsed back to the same value. */
    @Test
    void parsesWhatExporterEscapes() throws DataStorageException {
        List<String> values = List.of("Анна Литвин", "Коваль, Петро", "Олена \"Спорт\"", "\"", ",",
                "рядок 1\nрядок 2", "рядок 1\r\nрядок 2", "", " пробіли ");
        for (String value : values) {
            String csv = CsvExporter.escape(value) + "," + CsvExporter.escape(value) + "\n";
            assertEquals(List.of(List.of(value, value)), CsvParser.parseDocument(csv), value);
        }
    }

    /* Format error: an unclosed quote names the line where the quote opened. */
    @Test
    void rejectsUnclosedQuote() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("клієнт,тип\n\"Анна Литвин,MONTHLY\n"));

        assertEquals("line 2: quote is not closed", error.getMessage());
    }

    /* Format error: text after a closing quote means an undoubled quote inside the field. */
    @Test
    void rejectsTextAfterClosingQuote() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("a,b\n\"Олена \"Спорт\"\",1\n"));

        assertTrue(error.getMessage().startsWith("line 2: character 'С' after a closing quote"), error.getMessage());
    }

    /* A file is read as UTF-8; a byte order mark from a Windows editor is ignored. */
    @Test
    void readsUtf8FileWithByteOrderMark(@TempDir Path folder) throws IOException, DataStorageException {
        Path file = Files.writeString(folder.resolve("bom.csv"), "﻿клієнт,тип\r\nАнна Литвин,MONTHLY\r\n",
                StandardCharsets.UTF_8);

        assertEquals(List.of(List.of("клієнт", "тип"), List.of("Анна Литвин", "MONTHLY")), CsvParser.read(file));
    }

    /* File error: a missing file keeps the IOException as the cause. */
    @Test
    void reportsMissingFileWithCause(@TempDir Path folder) {
        Path missing = folder.resolve("missing.csv");

        DataStorageException error = assertThrows(DataStorageException.class, () -> CsvParser.read(missing));

        assertEquals("cannot read CSV file " + missing, error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
    }

    /* A broken file is reported with its path; the parser error is the cause. */
    @Test
    void reportsBrokenFileWithPath(@TempDir Path folder) throws IOException {
        Path file = Files.writeString(folder.resolve("broken.csv"), "a,\"b\n", StandardCharsets.UTF_8);

        DataStorageException error = assertThrows(DataStorageException.class, () -> CsvParser.read(file));

        assertEquals("broken CSV file " + file + ": line 1: quote is not closed", error.getMessage());
        assertInstanceOf(DataStorageException.class, error.getCause());
    }
}
