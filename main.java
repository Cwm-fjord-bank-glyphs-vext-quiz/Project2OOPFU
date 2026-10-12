import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {
 
   // How dates are shown to the user in reports (input is typed as YYYY-MM-DD)
   private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
 
   public static void main(String[] args) {
       Scanner scanner = new Scanner(System.in);
 
       String ticker = askTicker(scanner);
       String[] range = askDateRange(scanner);
       String startDate = range[0];
       String ending = range[1];
 
       ArrayList<DayRecordStock> records = loadRecords(ticker, startDate, ending);
 
       boolean running = true;
       while (running) {
           System.out.println();
           new Formater();
           System.out.println("Enter your choice:");
           String choice = scanner.nextLine().trim();
 
           switch (choice) {
               case "1":
                   ticker = askTicker(scanner);
                   records = loadRecords(ticker, startDate, ending);
                   break;
               case "2":
                   range = askDateRange(scanner);
                   startDate = range[0];
                   ending = range[1];
                   records = loadRecords(ticker, startDate, ending);
                   break;
               case "3": case "4": case "5": case "6": case "7":
               case "8": case "9": case "10": case "11":
                   if (records.isEmpty()) {
                       System.out.println("No data loaded for that ticker and period. Use option 1 or 2 to change them.");
                   } else {
                       runOption(choice, ticker, startDate, ending, records);
                   }
                   break;
               case "0":
                   running = false;
                   break;
               default:
                   System.out.println("Invalid choice. Please enter a number from the menu.");
           }
       }
       System.out.println("Goodbye!");
   }
 
   // ---------- Input helpers ----------
 
   // Keeps asking until the ticker is well-formed AND a data file exists for it
   private static String askTicker(Scanner scanner) {
       while (true) {
           System.out.println("Enter a ticker");
           String ticker = scanner.nextLine().trim().toUpperCase();
 
           if (!ticker.matches("[A-Z0-9.\\-]{1,10}")) {
               System.out.println("Invalid ticker symbol. Use letters/numbers only, e.g. NVDA.");
           } else if (!ReadFile.fileExists(ticker)) {
               System.out.println("No data file found for " + ticker + ". Check the symbol and try again.");
           } else {
               return ticker;
           }
       }
   }
 
   // Asks for a start and end date; repeats until both are real dates and start <= end
   private static String[] askDateRange(Scanner scanner) {
       while (true) {
           String start = askDate(scanner, "Enter the starting date range");
           String end = askDate(scanner, "Enter the ending date range");
 
           if (LocalDate.parse(start).isAfter(LocalDate.parse(end))) {
               System.out.println("Invalid range: the start date is after the end date. Please try again.");
           } else {
               return new String[] { start, end };
           }
       }
   }
 
   private static String askDate(Scanner scanner, String prompt) {
       while (true) {
           System.out.println(prompt + " (YYYY-MM-DD)");
           String input = scanner.nextLine().trim();
           try {
               LocalDate.parse(input); // just validates the format and that the date exists
               return input;
           } catch (DateTimeParseException e) {
               System.out.println("Invalid date. Example: 2025-01-31");
           }
       }
   }
 
   // ---------- Loading ----------
 
   // Uses ReadFile to get the matching lines, then turns them into DayRecordStock objects
   private static ArrayList<DayRecordStock> loadRecords(String ticker, String start, String end) {
       ArrayList<String> data = ReadFile.read(ticker, start, end);
       ArrayList<DayRecordStock> records = new ArrayList<>();
       int unreadable = 0;
 
       for (String line : data) {
           DayRecordStock record = DayRecordStock.fromCsvLine(line);
           if (record == null) {
               unreadable++;
           } else {
               records.add(record);
           }
       }
 
       records.sort(Comparator.comparing(DayRecordStock::getDate));
 
       if (unreadable > 0) {
           System.out.println("Note: skipped " + unreadable + " row(s) that could not be read.");
       }
       if (records.isEmpty()) {
           System.out.println("No trading records found for " + ticker + " between "
                   + formatDate(start) + " and " + formatDate(end) + ".");
       } else {
           System.out.println("Loaded " + records.size() + " trading days for " + ticker + ".");
       }
       return records;
   }
 
   // ---------- Menu actions ----------
 
   private static void runOption(String choice, String ticker, String start, String end,
                                 ArrayList<DayRecordStock> records) {
       RecordCalculator analyzer = new RecordCalculator(records);
       System.out.println();
 
       switch (choice) {
           case "3":
               printLine("Highest Closing Price:", money(analyzer.getHighestClose()));
               break;
           case "4":
               printLine("Lowest Closing Price:", money(analyzer.getLowestClose()));
               break;
           case "5":
               printLine("Average Closing Price:", money(analyzer.getAverageClose()));
               break;
           case "6":
               printLine("Price Change:", percent(analyzer.getPercentChange()));
               break;
           case "7":
               printLine("Best Trading Day:", dayText(analyzer.getBestDay()));
               break;
           case "8":
               printLine("Worst Trading Day:", dayText(analyzer.getWorstDay()));
               break;
           case "9":
               printRecords(records);
               break;
           case "10":
               printLine("Average Trading Volume:", volume(analyzer.getAverageVolume()));
               break;
           case "11":
               printReport(ticker, start, end, analyzer);
               break;
       }
   }
 
   private static void printRecords(List<DayRecordStock> records) {
       System.out.printf("%-12s %10s %10s %10s %10s %15s%n",
               "Date", "Open", "High", "Low", "Close", "Volume");
       for (DayRecordStock r : records) {
           System.out.printf(Locale.US, "%-12s %10.2f %10.2f %10.2f %10.2f %,15d%n",
                   r.getDate().format(DISPLAY_DATE), r.getOpen(), r.getHigh(),
                   r.getLow(), r.getClose(), r.getVolume());
       }
   }
 
   private static void printReport(String ticker, String start, String end, RecordCalculator a) {
       System.out.println("Ticker: " + ticker);
       System.out.println("Period: " + formatDate(start) + " - " + formatDate(end));
       System.out.println();
       printLine("Highest Closing Price:", money(a.getHighestClose()));
       printLine("Lowest Closing Price:", money(a.getLowestClose()));
       printLine("Average Closing Price:", money(a.getAverageClose()));
       printLine("Price Change:", percent(a.getPercentChange()));
       printLine("Average Trading Volume:", volume(a.getAverageVolume()));
       System.out.println();
       printLine("Best Trading Day:", dayText(a.getBestDay()));
       printLine("Worst Trading Day:", dayText(a.getWorstDay()));
   }
 
   // ---------- Formatting helpers ----------
 
   private static void printLine(String label, String value) {
       System.out.printf("%-24s%s%n", label, value);
   }
 
   private static String money(double value) {
       return String.format(Locale.US, "$%,.2f", value);
   }
 
   private static String percent(double value) {
       return String.format(Locale.US, "%+.2f%%", value);
   }
 
   private static String volume(double value) {
       return String.format(Locale.US, "%,.0f", value);
   }
 
   private static String dayText(DayChange day) {
       if (day == null) {
           return "n/a (need at least 2 trading days)";
       }
       return day.getDate().format(DISPLAY_DATE) + " (" + percent(day.getPercent()) + ")";
   }
 
   private static String formatDate(String isoDate) {
       return LocalDate.parse(isoDate).format(DISPLAY_DATE);
   }
}
 
// Menu: your original options 1-8 are unchanged; 9-11 and 0 were added
class Formater {
   public Formater() {
       System.out.println("Please Select from the Options below: ");
       System.out.println("1. Search for stock with Ticker");
       System.out.println("2. Specify date range for analysis");
       System.out.println("3. Calculate the highest closing price within a period");
       System.out.println("4. Calculate the lowest closing price within a period");
       System.out.println("5. Calculate the average closing price");
       System.out.println("6. Calculate percentage price change over period");
       System.out.println("7. Identify trading day with largest percentage increase");
       System.out.println("8. Identify trading day with largest percentage decrease");
       System.out.println("9. Display historical records for the selected stock");
       System.out.println("10. Calculate the average trading volume");
       System.out.println("11. Show full report");
       System.out.println("0. Exit");
   }
}
 
class ReadFile {
 
   private static final String FOLDER = "C:/Users/casas/Downloads/Market data_2025/";
 
   private static String pathFor(String ticker) {
       return FOLDER + ticker + "_2025.csv";
   }
 
   // Lets Main check for a missing file / unknown ticker before reading
   public static boolean fileExists(String ticker) {
       return new File(pathFor(ticker)).exists();
   }
 
   // This is a METHOD, not a constructor
   public static ArrayList<String> read(String ticker, String startingDate, String endingDate) {
 
       ArrayList<String> csvData = new ArrayList<>();
 
       try {
           String path = pathFor(ticker);
 
           BufferedReader csvReader = new BufferedReader(new FileReader(path));
 
           String line;
 
           // Skip header
           csvReader.readLine();
 
           while ((line = csvReader.readLine()) != null) {
 
               String[] row = line.split(",");
               String date = row[0].trim();
 
               // Correct date-range check
               if (date.compareTo(startingDate) >= 0 && date.compareTo(endingDate) <= 0) {
                   csvData.add(line);
               }
           }
 
           csvReader.close();
 
       } catch (Exception e) {
           e.printStackTrace();
       }
 
       return csvData;
   }
}
 
