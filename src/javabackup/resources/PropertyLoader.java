/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javabackup.resources;

import java.util.*;
import java.io.*;

/**
 *
 * @author  mce
 */
public class PropertyLoader {
    public static final Properties loadProperties(String fileName) {
        // http://www.javapractices.com/topic/TopicAction.do?Id=42
        Properties config = new Properties();

        // read the properties file
        File properties_file = new File(fileName);

        if(!properties_file.exists()) {
            return null;
        }

        try {
            Scanner scanner = new Scanner(new FileInputStream(fileName));
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();	
                if(line.matches("^#.*$")) {
                    continue;
                }
                if(line.contains("=")) {
                    String[] kv = line.split("=");
                    config.setProperty(kv[0], kv[1]);
                } else {
                    System.out.println("Invalid config line: " + line);
                    continue;
                }

            }
        
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        // set the properties in config

        return config;
    }
} // End of class
