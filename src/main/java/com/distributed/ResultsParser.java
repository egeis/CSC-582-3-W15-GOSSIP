package main.java.com.distributed;

/**
 *
 * @author Richard Coan
 */
public class ResultsParser {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        ArrayList<String> updates = new ArrayList<String>();
        Files.walk(Paths.get(System.getProperty("user.dir")+"/results/updates/")).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                    System.out.println(filePath);
                    
                    Scanner scanner;
                    try {
                        scanner = new Scanner(filePath.toFile());
                        while (scanner.hasNextLine()) {
                            updates.add(scanner.nextLine());
                        }
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
                    }
            }
        }
        
        Map<Integer, List<String> databases = new HashMap();
        Files.walk(Paths.get(System.getProperty("user.dir")+"/results/database/")).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                    System.out.println(filePath);
                    Integer key = Integer.parseInt(filePath.toString().split("_")[0]);
                    
                    Scanner scanner;
                    List<String> lines = new ArrayList<String>();
                    try {
                        scanner = new Scanner(filePath.toFile());
                        while (scanner.hasNextLine()) {
                            lines.add(scanner.nextLine());
                        }
                        
                        databases.put(key,lines);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(Launch.class.getName()).log(Level.SEVERE, null, ex);
                    }
            }
        }
    }
    
}
