package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

class MembershipTest {

    // 1. Адаптовані старі тести створення та валідації
    @Test
    void testValidMonthlyMembershipCreation() {
        MonthlyMembership membership = new MonthlyMembership("Іван", "Monthly", 1, 10, 1205.2);
        
        assertEquals("Іван", membership.getClient());
        assertEquals(MembershipKind.MONTHLY, membership.getKind());
    }

    @Test
    void testNegativePriceThrowsException() {
        // Перевіряємо, що підтип відхиляє некоректні дані (твоя стара перевірка)
        assertThrows(IllegalArgumentException.class, () -> {
            new AnnualMembership("Олег", "Annual", 12, 10, -1500.0);
        });
    }

    @Test
    void testBaseTypeRejectsInvalidCommonState() {
        // Перевіряємо логіку абстрактного класу через його підтип
        assertThrows(IllegalArgumentException.class, () -> {
            new MonthlyMembership("", "Monthly", 1, 10, 1500.0); // Порожнє ім'я
        });
    }

    // 2. Тести поліморфічної поведінки[cite: 1]
    @Test
    void testSubtypesCalculateCostDifferently() {
        Membership monthly = new MonthlyMembership("Олег", "Monthly", 1, 10, 1500.0); // 1500 / 10 = 150.0
        Membership annual = new AnnualMembership("Олег", "Annual", 12, 10, 12000.0); // (12000 / 12) / 10 = 100.0
        
        assertNotEquals(monthly.visitCost(), annual.visitCost());
    }

    // 3. Тести логічної рівності (equals / hashCode)[cite: 1]
    @Test
    void testEqualObjectsHaveOneSetEntry() {
        Set<Membership> uniqueMemberships = new HashSet<>();
        
        uniqueMemberships.add(new MonthlyMembership("Олег", "Monthly", 1, 10, 1500.0));
        uniqueMemberships.add(new MonthlyMembership("Олег", "Monthly", 1, 12, 1700.0)); // Інша ціна, але логічно це той самий клієнт
        
        // HashSet не повинен дублювати логічно рівні об'єкти[cite: 1]
        assertEquals(1, uniqueMemberships.size());
    }

    @Test
    void testDifferentSubtypesAreNotEqual() {
        Membership monthly = new MonthlyMembership("Олег", "Monthly", 1, 10, 1500.0);
        Membership annual = new AnnualMembership("Олег", "Annual", 12, 10, 1500.0);
        
        // Два об'єкти різних підтипів не є рівними за однакової назви[cite: 1]
        assertNotEquals(monthly, annual);
    }
}