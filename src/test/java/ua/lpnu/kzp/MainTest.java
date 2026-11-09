package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Smoke tests for the application entry point. */
class MainTest {

    /* Tests run from compiled classes, not from the JAR, so there is no manifest version. */
    @Test
    void versionFallsBackToDevOutsideJar() {
        assertEquals("dev", Main.appVersion());
    }
}
