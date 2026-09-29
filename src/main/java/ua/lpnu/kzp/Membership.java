package ua.lpnu.kzp;

import java.util.Objects;
import java.util.Locale;

public abstract class Membership {
    private final String client;
    private final MembershipKind kind;

    protected Membership(String client, MembershipKind kind){
        if (client == null || client.isBlank()){
            throw new IllegalArgumentException("Name of client is not defined");
            }
        this.client = client;
        this.kind = Objects.requireNonNull(kind, "Kind cannot be null");
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