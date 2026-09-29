package ua.lpnu.kzp;

public record MembershipSummary(
    int validCount, 
    double averageVisits, 
    double totalRevenue, 
    int maxMonths
) {
    public MembershipSummary {
        if (validCount < 0) {
            throw new IllegalArgumentException("Cannot be negative.");
        }
        if (totalRevenue < 0){
            throw new IllegalArgumentException("Cannot be negative.");
        }
        if (maxMonths < 0) {
            throw new IllegalArgumentException("Cannot be negative.");
        }
        if (averageVisits < 0 || !Double.isFinite(averageVisits)){
            throw new IllegalArgumentException("Cannot be negative.");
        }
    }
}