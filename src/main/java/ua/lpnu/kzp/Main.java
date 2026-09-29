package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 
 * Головний клас програми для обробки бази тренажерного залу. 
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.out.println("Старт обробки бази тренажерного залу!");
        Path filePath = Path.of("data", "input.csv");
        
        if (args.length > 0 && "--version".equals(args[0])) {
            System.out.println("Gym Trainer App v1.1.0 (Polymorphic Edition)");
            return; 
        }
        
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            System.out.println("Успішно прочитано рядків: " + lines.size());

            List<Membership> memberships = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                try {
                    // Використовуємо наш новий фабричний метод
                    memberships.add(parseCsv(line));
                } catch (IllegalArgumentException e) {
                    errors.add("Пропущено рядок " + (i + 1) + ": " + e.getMessage());
                }
            }

            // ФАЗА ПОЛІМОРФІЗМУ
            double totalVisitCost = 0.0;
            double maxVisitCost = 0.0;
            
            for (Membership m : memberships) {
                double cost = m.visitCost();
                totalVisitCost += cost;
                maxVisitCost = Math.max(maxVisitCost, cost);
            }

            // Демонстрація роботи equals/hashCode через HashSet
            Set<Membership> uniqueMemberships = new HashSet<>(memberships);

            // ФАЗА ФОРМУВАННЯ ЗВІТУ
            String report = String.format(Locale.ROOT,
                    "%n--- ЗВІТ ---%n" +
                    "Всього оброблено записів: %d%n" +
                    "Унікальних клієнтів (завдяки HashSet): %d%n" +
                    "Найвища вартість одного візиту: %.2f грн%n" +
                    "Сумарна вартість усіх візитів (поліморфно): %.2f грн%n",
                    memberships.size(), uniqueMemberships.size(), maxVisitCost, totalVisitCost);

            System.out.println(report);
            
            if (!errors.isEmpty()) {
                System.out.println("Помилок: " + errors.size());
                errors.forEach(System.out::println);
            }

            Path outputPath = Path.of("out", "report.txt");
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, report, StandardCharsets.UTF_8);
            
        } catch (IOException e) {
            System.out.println("Сталася помилка при читанні файлу: " + e.getMessage());
        }
    }

    /**
     * Фабричний метод, який замінює старий Membership.fromCsv.
     * аналізує рядок і створює правильний підтип.
     */
    private static Membership parseCsv(String line) {
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("Некоректна кількість елементів (очікується 5).");
        }
        
        try {
            String client = fields[0];
            String plan = fields[1];
            int months = Integer.parseInt(fields[2]);
            int visits = Integer.parseInt(fields[3]);
            double price = Double.parseDouble(fields[4]);

            // Логіка маршрутизації:
            if ("Monthly".equalsIgnoreCase(plan) || months == 1) {
                return new MonthlyMembership(client, price, visits);
            } else {
                // Якщо річний, то вираховуємо середню кількість візитів на місяць
                int visitsPerMonth = Math.max(1, visits / months);
                return new AnnualMembership(client, price, months, visitsPerMonth);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Помилка парсингу числових значень.");
        }
    }
}