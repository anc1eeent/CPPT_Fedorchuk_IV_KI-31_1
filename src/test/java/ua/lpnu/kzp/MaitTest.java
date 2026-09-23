package ua.lpnu.kzp;

// Імпортуємо метод для порівняння очікуваного та фактичного результатів
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class MainTest {
    
    /* Перевіряє базове арифметичне обчислення тестового каркаса. */
    @Test
    void sampleCalculationIsCorrect() {
        // Smoke-test підтверджує підключення JUnit
        assertEquals(4, 2 + 2);
    }
}