package ua.lpnu.kzp;

/** Річний абонемент. */
public final class AnnualMembership extends Membership {


    /** Створює річний абонемент. */
    public AnnualMembership(String client, String plan, int months, int visits, double price) {
        super(client, plan, months, visits, price, MembershipKind.ANNUAL);
        if (months <= 0 || months > 12) {
            throw new IllegalArgumentException("Months could be from 1 to 12.");
        }
    }

    /** Обчислює вартість одного відвідування з урахуванням тривалості. */
    @Override
    public double visitCost() {
        int visitsPerMonth = Math.max(1, getVisits() / getMonths());
        return (getPrice() / getMonths()) / visitsPerMonth;
    }
}