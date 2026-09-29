package ua.lpnu.kzp;

public final class MonthlyMembership extends Membership {
    private final double monthlyPrice;
    private final int visits;

    public MonthlyMembership(String client, double montlyPrice, int visits){
        super(client, MembershipKind.MONTHLY);
        
        if (montlyPrice < 0 || !Double.isFinite(montlyPrice)){
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (visits <= 0){
            throw new IllegalArgumentException("Visits cannot be less than 1.");
        }
        this.monthlyPrice = montlyPrice;
        this.visits = visits;
    }

    @Override 
    public double visitCost(){
        return monthlyPrice / visits;
    }
}
