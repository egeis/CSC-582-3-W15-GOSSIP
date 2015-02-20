package main.java.com.distributed.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes files to a specified path.
 * @author Richard Coan
 */
public class FileIO {    
   
    /**
     * Writes a file from the contents.
     * @param filename to be written to.
     * @param contents of file to be written.
     */
    public static void WriteFile(String filename, String contents)
    {
        try {
            Files.write(Paths.get("./"+filename+".txt"), contents.getBytes());
        } catch(IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Reads a file and returns a string.
     * @param path to the file.
     * @return string contents of the file.
     */
    public static String ReadFile(String path)
    {
        Logger.getLogger(FileIO.class.getName()).log(Level.INFO, "Loading File: "+path);
        
        ClassLoader classLoader = FileIO.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(path);
        StringBuilder contents = new StringBuilder();

        try {            
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String text = null;
            
            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
                contents.append(text).append(
                System.getProperty("line.separator"));
            }
            
            reader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return contents.toString();
    }
    
    /**
     * Test for File Reader and Parser.
     * @param args ignored.
     */
    public static void main(String[] args) {
        /** Loading File Test, using Test Data #3 **/
        String path = "main\\resources\\init_data.txt";
        System.out.println("Reading File: "+path);
        String contents = ReadFile(path);
        System.out.println(contents);
    }

}
