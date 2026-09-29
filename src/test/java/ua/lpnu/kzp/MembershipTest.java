package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MembershipTest {

    @Test
    void testValidMembershipCreation() {
        // Arrange (Підготовка)
        String client = "Іван";
        String plan = "Gold";
        int months = 12;
        int visits = 100;
        double price = 5000.0;

        // Act (Дія)
        Membership membership = new Membership(client, plan, months, visits, price);
        // Assert (Перевірка)
        assertEquals("Іван", membership.getClient());
        assertEquals(5000.0, membership.getPrice());
    }

    @Test
    void testNegativePriceThrowsException() {
        // Arrange
        double negativePrice = -100.0;

        // Act & Assert (Для винятків ці дві фази об'єднуються)
        assertThrows(IllegalArgumentException.class, () -> {
            new Membership("Іван", "Gold", 12, 100, negativePrice);
        });
        
    }

    @Test
    void testValidFromCsvParsing() {
        String csvLine = "Олег;Start;1;12;500.50";
    }
}