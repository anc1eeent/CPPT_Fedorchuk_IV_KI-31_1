package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

class MembershipTest {

    // 1. Адаптовані старі тести створення та валідації
    @Test
    void testValidMonthlyMembershipCreation() {
        MonthlyMembership membership = new MonthlyMembership("Іван", 1500.0, 12);
        
        assertEquals("Іван", membership.getClient());
        assertEquals(MembershipKind.MONTHLY, membership.getKind());
    }

    @Test
    void testNegativePriceThrowsException() {
        // Перевіряємо, що підтип відхиляє некоректні дані (твоя стара перевірка)
        assertThrows(IllegalArgumentException.class, () -> {
            new AnnualMembership("Іван", -5000.0, 12, 10);
        });
    }

    @Test
    void testBaseTypeRejectsInvalidCommonState() {
        // Перевіряємо логіку абстрактного класу через його підтип
        assertThrows(IllegalArgumentException.class, () -> {
            new MonthlyMembership("", 1500.0, 10); // Порожнє ім'я
        });
    }

    // 2. Тести поліморфічної поведінки[cite: 1]
    @Test
    void testSubtypesCalculateCostDifferently() {
        Membership monthly = new MonthlyMembership("Олег", 1500.0, 10); // 1500 / 10 = 150.0
        Membership annual = new AnnualMembership("Марія", 12000.0, 12, 10); // (12000 / 12) / 10 = 100.0
        
        assertNotEquals(monthly.visitCost(), annual.visitCost());
    }

    // 3. Тести логічної рівності (equals / hashCode)[cite: 1]
    @Test
    void testEqualObjectsHaveOneSetEntry() {
        Set<Membership> uniqueMemberships = new HashSet<>();
        
        uniqueMemberships.add(new MonthlyMembership("Іван", 1500.0, 12));
        uniqueMemberships.add(new MonthlyMembership("Іван", 2000.0, 15)); // Інша ціна, але логічно це той самий клієнт
        
        // HashSet не повинен дублювати логічно рівні об'єкти[cite: 1]
        assertEquals(1, uniqueMemberships.size());
    }

    @Test
    void testDifferentSubtypesAreNotEqual() {
        Membership monthly = new MonthlyMembership("Іван", 1500.0, 12);
        Membership annual = new AnnualMembership("Іван", 12000.0, 12, 10);
        
        // Два об'єкти різних підтипів не є рівними за однакової назви[cite: 1]
        assertNotEquals(monthly, annual);
    }
}