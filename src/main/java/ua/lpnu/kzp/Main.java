package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 
 * Головний клас програми для обробки бази тренажерного залу. 
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.out.println("Старт обробки бази тренажерного залу!");
        Path filePath = Path.of("data", "input.csv");
        
        if (args.length > 0 && "--version".equals(args[0])) {
            System.out.println("Gym Trainer App v1.0.0");
            return; 
        }
        
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            System.out.println("Успішно прочитано рядків: " + lines.size());

            // 1. Створюємо списки для чистих даних та помилок
            List<Membership> memberships = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                try{
                   memberships.add(Membership.fromCsv(line));
                } catch (IllegalArgumentException e) {
                    errors.add("Пропущено рядок: " + (i + 1) + ": " + e.getMessage());
                }
            }

            double totalRevenue = 0.0;
            int totalVisits = 0;
            int maxMonths = 0;
           
            for(Membership m : memberships){
                totalRevenue = totalRevenue + m.getPrice();
                totalVisits = totalVisits + m.getVisits();
                maxMonths = Math.max(maxMonths, m.getMonths());
            }

            // ФАЗА 3: Пакування у record
            double averageVisits = memberships.isEmpty() ? 0.0 : (double) totalVisits / memberships.size();
            
            MembershipSummary summary = new MembershipSummary(memberships.size(), averageVisits, totalRevenue, maxMonths);
            
            // ФАЗА 4: Формування звіту
            String report = String.format(Locale.ROOT,
                    "%n--- ЗВІТ ---%n" +
                    "Коректних записів: %d%n" +
                    "Загальний виторг: %.2f%n" +
                    "Найдовший абонемент (місяців): %d%n",
                    summary.validCount(), summary.totalRevenue(), summary.maxMonths());

            if (summary.validCount() > 0) {
                report += String.format(Locale.ROOT, "Середня кількість відвідувань: %.2f%n", averageVisits);
            }
            System.out.println(report);

            Path outputPath = Path.of("out", "report.txt");
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // ... (твій старий код запису у файл)

        } catch (IOException e) {
            System.out.println("Сталася помилка при читанні файлу: " + e.getMessage());
        }
    }
}