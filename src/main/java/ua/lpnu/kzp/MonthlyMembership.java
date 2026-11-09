package ua.lpnu.kzp;

public final class MonthlyMembership extends Membership {
 

   public MonthlyMembership(String client, String plan, int months, int visits, double price){
        super(client, plan, months, visits, price, MembershipKind.MONTHLY);
    }

    @Override 
    public double visitCost(){
        return getPrice() / getVisits();
    }
}
