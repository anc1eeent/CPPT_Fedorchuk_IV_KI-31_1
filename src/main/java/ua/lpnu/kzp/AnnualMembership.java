package ua.lpnu.kzp;

/** Річний абонемент. */
public final class AnnualMembership extends Membership {
    private final double annualPrice;
    private final int months;
    private final int visitsPerMonth;

    /** Створює річний абонемент. */
    public AnnualMembership(String client, double annualPrice, int months, int visitsPerMonth) {
        // Передаємо спільні дані до базового класу
        super(client, MembershipKind.ANNUAL);
        
        // Валідація
        if (annualPrice < 0 || !Double.isFinite(annualPrice)) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (months <= 0 || months > 12) {
            throw new IllegalArgumentException("Months could be from 1 to 12.");
        }
        if (visitsPerMonth <= 0) {
            throw new IllegalArgumentException("Visits cannot be negative.");
        }
        
        this.annualPrice = annualPrice;
        this.months = months;
        this.visitsPerMonth = visitsPerMonth;
    }

    /** Обчислює вартість одного відвідування з урахуванням тривалості. */
    @Override
    public double visitCost() {
        // Використовуємо твою логіку: (price / months) / visitsPerMonth
        return (annualPrice / months) / visitsPerMonth;
    }
}