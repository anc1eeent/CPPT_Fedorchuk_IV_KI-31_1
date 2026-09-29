package ua.lpnu.kzp;

import java.util.Objects;

public abstract class Membership {
    private final String client;
    private final MembershipKind kind;

    /** Створює абонемент із перевіреними спільними полями. */
    protected Membership(String client, MembershipKind kind) {
        // Спочатку валідуємо, потім передаємо в приватний конструктор[cite: 1]
        this(validateClient(client), Objects.requireNonNull(kind, "Категорія абонемента не може бути null"), true);
    }

    /* Присвоює вже перевірений стан елемента. Конструктор без винятків. */
    private Membership(String client, MembershipKind kind, boolean validated) {
        this.client = client;
        this.kind = kind;
    }

    /* Статичний метод для перевірки імені до створення об'єкта. */
    private static String validateClient(String client) {
        if (client == null || client.isBlank()) {
            throw new IllegalArgumentException("Ім'я клієнта не може бути порожнім.");
        }
        return client;
    }

    public final String getClient() {
        return client;
    }

    public final MembershipKind getKind() {
        return kind;
    }

    /** 
     * Поліморфна операція. 
     * Кожен підтип рахуватиме вартість відвідування по-своєму.
     */
    public abstract double visitCost();

    // equals та hashCode згідно методички реалізовуємо тут, 
    // щоб порівнювати логічну рівність об'єктів.

    /** Порівнює абонементи за точним класом (підтипом) та іменем клієнта. */
    @Override
    public final boolean equals(Object other) {
        // Якщо це одне й те саме посилання в пам'яті — об'єкти точно рівні
        if (this == other) {
            return true;
        }
        // getClass() не допускає рівності різних підтипів
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Membership that = (Membership) other;
        // Логічно абонементи рівні, якщо вони одного типу і належать одному клієнту
        return client.equals(that.client);
    }

    /** Повертає хеш-код, узгоджений із логікою equals. */
    @Override
    public final int hashCode() {
        // Беремо ті самі поля, що й в equals
        return Objects.hash(getClass(), client);
    }
}