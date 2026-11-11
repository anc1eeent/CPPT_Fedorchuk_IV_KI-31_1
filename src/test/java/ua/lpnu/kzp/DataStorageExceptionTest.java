package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/** Tests for the storage exception. */
class DataStorageExceptionTest {

    /* A format error has a message and no technical cause. */
    @Test
    void keepsMessageWithoutCause() {
        DataStorageException error = new DataStorageException("expected 5 fields, got 4");

        assertEquals("expected 5 fields, got 4", error.getMessage());
        assertNull(error.getCause());
    }

    /* A wrapped error stays reachable through getCause. */
    @Test
    void keepsOriginalErrorAsCause() {
        IOException original = new IOException("disk is full");

        DataStorageException error = new DataStorageException("cannot write CSV file out/memberships.csv", original);

        assertEquals("cannot write CSV file out/memberships.csv", error.getMessage());
        assertSame(original, error.getCause());
    }

    /* The exception is checked: the compiler makes every caller handle or declare it. */
    @Test
    void isCheckedException() {
        assertTrue(Exception.class.isAssignableFrom(DataStorageException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(DataStorageException.class));
    }
}
