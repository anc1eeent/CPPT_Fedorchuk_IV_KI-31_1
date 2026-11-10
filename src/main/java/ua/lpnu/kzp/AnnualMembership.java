package ua.lpnu.kzp;

import java.time.LocalDate;

/**
 * Membership for several months, up to a year.
 *
 * <p>A visit costs the monthly share of the price divided by the whole number
 * of visits per month.</p>
 */
public final class AnnualMembership extends Membership {

    /** Longest allowed duration in months. */
    public static final int MAX_MONTHS = 12;

    /**
     * Creates an annual membership.
     *
     * @param client    client full name, must not be blank
     * @param plan      tariff name, must not be blank
     * @param months    duration in months, from 1 to {@value #MAX_MONTHS}
     * @param visits    number of visits, at least 1
     * @param price     total price paid, finite and not negative
     * @param startDate first day of the membership, must not be null
     * @throws IllegalArgumentException if a field is invalid
     */
    public AnnualMembership(String client, String plan, int months, int visits, double price,
            LocalDate startDate) {
        super(client, plan, months, visits, price, startDate, MembershipKind.ANNUAL);
        // Throwing after super() is safe here: the class is final, so no subclass
        // can get hold of a half-built object.
        if (months > MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "months must not exceed " + MAX_MONTHS + ", got " + months);
        }
    }

    /**
     * Calculates the cost of one visit as {@code (price / months) / visitsPerMonth}.
     *
     * <p>{@code visitsPerMonth} uses integer division and is at least 1,
     * which keeps the values of the Lab 3 report unchanged.</p>
     *
     * @return cost of one visit
     */
    @Override
    public double visitCost() {
        int visitsPerMonth = Math.max(1, getVisits() / getMonths());
        return (getPrice() / getMonths()) / visitsPerMonth;
    }
}
