package ua.lpnu.kzp;

/**
 * Reports that data could not be saved, read or converted.
 *
 * <p>The exception is checked, so every caller of the storage layer must either handle it or
 * declare it. The message explains the operation and names the file, record or field; the
 * original technical error (for example {@link java.io.IOException} or
 * {@link NumberFormatException}) is kept as the cause and is available through
 * {@link #getCause()}.</p>
 *
 * <p>Error levels of the program:</p>
 * <ul>
 *   <li>model: a value breaks an invariant, {@link IllegalArgumentException} from a constructor;</li>
 *   <li>format: wrong number of CSV fields, wrong header, broken quotes, this exception;</li>
 *   <li>value: bad number, date or enum name, this exception with the parse error as the cause;</li>
 *   <li>file: the file cannot be read or written, this exception with the {@code IOException} as the cause;</li>
 *   <li>reflection: no annotated fields or no access to a field, this exception.</li>
 * </ul>
 */
public class DataStorageException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception for an error without a technical cause, for example a wrong field count.
     *
     * @param message what went wrong, with the file, record or field
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Creates an exception that wraps the original error.
     *
     * @param message what went wrong, with the file, record or field
     * @param cause   original error, kept for {@link #getCause()}
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
