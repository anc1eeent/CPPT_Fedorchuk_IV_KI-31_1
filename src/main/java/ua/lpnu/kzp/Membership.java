package ua.lpnu.kzp;

import java.util.Objects;
import java.util.Locale;

public final class Membership {
    private final String client;
    private final String plan;
    private final int months;
    private final int visits;
    private final double price;
    /**
     * Конструктор. Тут відбувається "Pydantic-стайл" валідація інваріантів.
     */
    public Membership(String client, String plan, int months, int visits, double price) {
            Objects.requireNonNull(client, "Client name cannot be null");
            Objects.requireNonNull(plan, "Plan cannot be null");
            if (client.isBlank()){
                throw new IllegalArgumentException("Name of client is not defined.");
            }
            if (plan.isBlank()){
                throw new IllegalArgumentException("Plan is not defined.");
            }
            if (months <= 0){
                throw new IllegalArgumentException("Months cannot be negative.");
            }
            if (visits < 0){
                throw new IllegalArgumentException("Visits cannot be negative.");
            }
            if (price < 0 || !Double.isFinite(price)){
                throw new IllegalArgumentException("Price cannot be negative.");
            }
            
            this.client = client;
            this.plan = plan;
            this.months = months;
            this.visits = visits;
            this.price = price;
        }

    /**
     * Статичний фабричний метод для створення об'єкта з сирого рядка CSV.
     */
    public static Membership fromCsv(String line) {
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("Less/more than 5 elements.");
        }
        try{
            String client = fields[0];
            String plan = fields[1];
            int months = Integer.parseInt(fields[2]);
            int visits = Integer.parseInt(fields[3]);
            double price = Double.parseDouble(fields[4]);
            return new Membership(client, plan, months, visits, price);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Cannot do parse operation");
        }
    }

    public String getClient(){
        return client;
    }

    public String getPlan(){
        return plan;
    }

    public int getMonths(){
        return months;
    }

    public int getVisits(){
        return visits;
    }

    public double getPrice(){
        return price;
    }
    
    @Override 
    public String toString(){
        return String.format(Locale.ROOT, 
            "Клієнт: %s, План: %s, %d міс., %d відвід., %.2f грн", 
            client, plan, months, visits, price);
    }
}
