package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class Main{

    private Main(){

    }

    public static void main(String[] args){
        System.out.println("Старт обробки бази тренажерного залу!");
        Path filePath = Path.of("data", "input.csv");

        try{
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            System.out.println("Успішон прочитано рядків: " + lines.size());

            int validCount = 0;
            double totalRevenue = 0.0;
            int totalVisits = 0;
            int maxMonths = 0;
            
            for (String line : lines) {
                String[] fields = line.split(";", -1);
                if (fields.length != 5){
                    System.out.println("Пропущено рядок (неправильна кількість полів): " + line);
                    continue;
                } 

               try {
                    String client = fields[0];
                    String plan = fields[1];

                    int months = Integer.parseInt(fields[2]);
                    int visits = Integer.parseInt(fields[3]);
                    double price = Double.parseDouble(fields[4]);

                    if (months < 0 || price < 0){
                        System.out.println("Не може бути від'ємним.");
                        continue;
                    } else { 
                    validCount++;
                    totalRevenue += price;
                    totalVisits += visits;
                    }
                    
                    if (months > maxMonths){
                        maxMonths = months;
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Рядок " + (line + 1) + " пропущено: нечислове значення");
                }
            }

           // 1. Формуємо єдиний текст звіту (DRY)
            // %d - ціле число, %.2f - дробове з двома нулями, %n - перехід на новий рядок
            String report = String.format(Locale.ROOT,
                    "%n--- ЗВІТ ---%n" +
                    "Коректних записів: %d%n" +
                    "Загальний виторг: %.2f%n" +
                    "Найдовший абонемент (місяців): %d%n",
                    validCount, totalRevenue, maxMonths);
            
            if (validCount > 0) {
                double averageVisits = (double) totalVisits / validCount;
                // Доклеюємо середнє значення до нашого звіту (+= працює як і в Python)
                report += String.format(Locale.ROOT, "Середня кількість відвідувань: %.2f%n", averageVisits);
            }

            // 2. Виводимо готовий звіт у консоль
            System.out.print(report);

            // 3. Записуємо готовий звіт у файл
            Path outputPath = Path.of("out", "report.txt");
            
          // Безпечно перевіряємо, чи є в шляху батьківська папка
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