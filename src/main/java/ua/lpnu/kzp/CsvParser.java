package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads CSV documents in the format written by {@link CsvExporter}.
 *
 * <p>{@code line.split(",")} is not a CSV parser: it breaks a quoted field such as
 * {@code "Коваль, Олена"} in two, and reading the file line by line breaks a quoted field that
 * contains a line break. This parser reads the whole text character by character as a state
 * machine and returns records, not physical lines.</p>
 *
 * <ul>
 *   <li>fields are separated by commas, records by {@code \n} or {@code \r\n};</li>
 *   <li>a field that starts with a double quote ends at the next single double quote; inside it
 *       commas and line breaks are data and {@code ""} is one quote;</li>
 *   <li>after a closing quote only a comma, a line break or the end of the text may follow;</li>
 *   <li>a quote in the middle of an unquoted field is kept as an ordinary character;</li>
 *   <li>a line break after the last record does not create an empty record.</li>
 * </ul>
 */
public final class CsvParser {

    /* Byte order mark that some Windows editors put at the start of a UTF-8 file. */
    private static final char BYTE_ORDER_MARK = '﻿';

    /* Where the parser is inside the document. */
    private enum State {
        /** Before the first character of a record. */
        RECORD_START,
        /** After a comma, before the first character of the next field. */
        FIELD_START,
        /** Inside a field that did not start with a quote. */
        UNQUOTED,
        /** Inside a quoted field. */
        QUOTED,
        /** Right after the quote that closed a quoted field. */
        AFTER_QUOTE
    }

    private CsvParser() {
    }

    /**
     * Reads a UTF-8 CSV file and splits it into records.
     *
     * @param path CSV file
     * @return records of the file, each a list of field values; empty for an empty file
     * @throws DataStorageException if the file cannot be read (the {@link IOException} is the cause)
     *                              or its CSV is broken
     * @throws NullPointerException if {@code path} is null
     */
    public static List<List<String>> read(Path path) throws DataStorageException {
        Objects.requireNonNull(path, "path must not be null");
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataStorageException("cannot read CSV file " + path, e);
        }
        if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
            text = text.substring(1);
        }
        try {
            return parseDocument(text);
        } catch (DataStorageException e) {
            throw new DataStorageException("broken CSV file " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Splits a CSV document into records and fields.
     *
     * @param text whole CSV document
     * @return unmodifiable list of records; each record is an unmodifiable list of field values;
     *         empty for empty text
     * @throws DataStorageException if a quote is not closed or a character follows a closing quote;
     *                              the message names the line
     * @throws NullPointerException if {@code text} is null
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        Objects.requireNonNull(text, "text must not be null");
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        State state = State.RECORD_START;
        int line = 1;
        int quoteLine = 0;

        for (int i = 0; i < text.length(); i++) {
            char symbol = text.charAt(i);

            if (state == State.QUOTED) {
                // Inside quotes everything is data; only a quote needs a decision.
                if (symbol == CsvExporter.QUOTE) {
                    if (i + 1 < text.length() && text.charAt(i + 1) == CsvExporter.QUOTE) {
                        field.append(CsvExporter.QUOTE);
                        i++;
                    } else {
                        state = State.AFTER_QUOTE;
                    }
                } else {
                    if (symbol == '\n') {
                        line++;
                    }
                    field.append(symbol);
                }
            } else if (symbol == CsvExporter.SEPARATOR) {
                fields.add(field.toString());
                field.setLength(0);
                state = State.FIELD_START;
            } else if (symbol == '\r' || symbol == '\n') {
                fields.add(field.toString());
                records.add(List.copyOf(fields));
                fields.clear();
                field.setLength(0);
                state = State.RECORD_START;
                // \r\n is one line break.
                if (symbol == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                line++;
            } else if (state == State.AFTER_QUOTE) {
                throw new DataStorageException("line " + line + ": character '" + symbol
                        + "' after a closing quote; a quote inside a quoted field must be doubled");
            } else if (symbol == CsvExporter.QUOTE && state != State.UNQUOTED) {
                state = State.QUOTED;
                quoteLine = line;
            } else {
                field.append(symbol);
                state = State.UNQUOTED;
            }
        }

        if (state == State.QUOTED) {
            throw new DataStorageException("line " + quoteLine + ": quote is not closed");
        }
        // Text that does not end with a line break still has a last record to finish.
        if (state != State.RECORD_START) {
            fields.add(field.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }
}
