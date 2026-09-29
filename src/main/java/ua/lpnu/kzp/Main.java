package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** 
 * Головний клас програми для обробки бази тренажерного залу. 
 */
public final class Main {

    private Main() {
    }

    /**
     * Точка входу до програми.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        System.out.println("Старт обробки бази тренажерного залу!");
        Path filePath = Path.of("data", "input.csv");
        
        // Перевірка версії (Вимога Рівня 3)
        if (args.length > 0 && "--version".equals(args[0])) {
            System.out.println("Gym Trainer App v1.0.0");
            return; 
        }
        
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            System.out.println("Успішно прочитано рядків: " + lines.size());

            int validCount = 0;
            double totalRevenue = 0.0;
            int totalVisits = 0;
            int maxMonths = 0;
            
            // Використовуємо індексований цикл, щоб знати номер рядка
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] fields = line.split(";", -1);
                
                if (fields.length != 5) {
                    System.out.println("Пропущено рядок " + (i + 1) + " (неправильна кількість полів): " + line);
                    continue;
                } 

                try {
                    String client = fields[0];
                    String plan = fields[1];

                    int months = Integer.parseInt(fields[2]);
                    int visits = Integer.parseInt(fields[3]);
                    double price = Double.parseDouble(fields[4]);

                    if (months < 0 || visits < 0 || price < 0) {
                        System.out.println("Рядок " + (i + 1) + " пропущено: значення не може бути від'ємним.");
                        continue;
                    } 
                    
                    validCount++;
                    totalRevenue += price;
                    totalVisits += visits;
                    
                    if (months > maxMonths) {
                        maxMonths = months;
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Рядок " + (i + 1) + " пропущено: нечислове значення");
                }
            }

            String report = String.format(Locale.ROOT,
                    "%n--- ЗВІТ ---%n" +
                    "Коректних записів: %d%n" +
                    "Загальний виторг: %.2f%n" +
                    "Найдовший абонемент (місяців): %d%n",
                    validCount, totalRevenue, maxMonths);
            
            if (validCount > 0) {
                double averageVisits = (double) totalVisits / validCount;
                report += String.format(Locale.ROOT, "Середня кількість відвідувань: %.2f%n", averageVisits);
            }

            System.out.print(report);

            Path outputPath = Path.of("out", "report.txt");
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, report, StandardCharsets.UTF_8);
            System.out.println("\nЗвіт успішно збережено у файл: " + outputPath);

        } catch (IOException e) {
            System.out.println("Сталася помилка при читанні файлу: " + e.getMessage());
        }
    }
}