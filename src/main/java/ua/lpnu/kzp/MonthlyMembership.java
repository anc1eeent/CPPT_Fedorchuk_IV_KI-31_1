package ua.lpnu.kzp;

import java.time.LocalDate;

/**
 * Membership for a single month.
 *
 * <p>A visit costs the whole price divided by the number of visits.</p>
 */
public final class MonthlyMembership extends Membership {

    /**
     * Creates a monthly membership.
     *
     * @param client    client full name, must not be blank
     * @param plan      tariff name, must not be blank
     * @param months    duration in months, at least 1
     * @param visits    number of visits, at least 1
     * @param price     total price paid, finite and not negative
     * @param startDate first day of the membership, must not be null
     * @throws IllegalArgumentException if a shared field is invalid
     */
    public MonthlyMembership(String client, String plan, int months, int visits, double price,
            LocalDate startDate) {
        super(client, plan, months, visits, price, startDate, MembershipKind.MONTHLY);
    }

    /**
     * Calculates the cost of one visit as {@code price / visits}.
     *
     * @return cost of one visit
     */
    @Override
    public double visitCost() {
        return getPrice() / getVisits();
    }
}
