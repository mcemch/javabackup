/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javabackup.resources;

import java.io.*;
import java.util.*;

/**
 *
 * @author mce
 */
public class RsyncCommander {
    private String rsync_cmd;
    private String rsync_opt;

    RsyncCommander() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            this.rsync_cmd = "rsync.exe ";
            this.rsync_opt = "-rti --out-format=\"%n%L\"    ";
        } else {
            this.rsync_cmd = "/usr/bin/rsync ";
            this.rsync_opt = "-a --out-format='%n%L'  ";
        }
    }

    private String createScript(String command) {
        BufferedWriter buf = null;
        File temp_file = null;

        try {
            temp_file = File.createTempFile("JavaBackup", ".script");
            temp_file.deleteOnExit();
            buf = new BufferedWriter(new FileWriter(temp_file));
            buf.write("#!/bin/bash\n");
            buf.write(command + "\n");
            buf.flush();
            buf.close();
            temp_file.setExecutable(true);
        } 
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return temp_file.getAbsolutePath();

    }

    public int rsyncGetTotal(String src, String dst, JavaBackup parent) {
        int total = 0;
        String cmd_opt = this.rsync_opt + " --dry-run   ";
        try {
            String command = this.rsync_cmd + cmd_opt + "\"" + src + "\" \"" + dst +"\"";
            Process p;
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                p = Runtime.getRuntime().exec(command);
            } else {
                // java on linux has a problem with quoted directory names in the argument list
                String script = this.createScript(command);
                p = Runtime.getRuntime().exec(script);
            }

		    Scanner sc = new Scanner(p.getInputStream());
		    while(sc.hasNext()) {
                sc.nextLine();
                total++;
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return total;
    }


    public int rsync(String src, String dst,JavaBackup parent) {
        String cmd_opt = this.rsync_opt;
        Process p = null;
        int total = 0;

        if (parent.schedule.dry_run_mode) {
            cmd_opt += this.rsync_opt + " --dry-run ";
        }
        else if (parent.schedule.delete_mode) {
            cmd_opt += this.rsync_opt + "    --delete    ";
        }

        try {
            String command = this.rsync_cmd + cmd_opt + "\"" + src + "\" \"" + dst +"\"";
            String script = this.createScript(command);

            if(System.getProperty("os.name").toLowerCase().contains("windows")) {
                p = Runtime.getRuntime().exec(command);
            } else {
                p = Runtime.getRuntime().exec(script);
            }

		    Scanner sc = new Scanner(p.getInputStream());
		    while(sc.hasNext()) {
                String line = sc.nextLine();
                parent.textAreaAppend(line.replaceAll("^[a-zA-Z+<>]+ ","")+"\n");
                parent.incrementProgress();
                
		    }

            // wait for the process to exit and print out any errors
            p.waitFor();
            if (p.exitValue() != 0) {
                Scanner sce = new Scanner(p.getErrorStream());
                System.out.println("Command:[" + p.exitValue() + "] " + command + "\n");
                System.out.println("Script: " + script + "\n");
                while(sce.hasNext()) {
                    System.out.println(sce.nextLine() + "\n");
                }
            }


        } 
        catch (IOException ex) {
            ex.printStackTrace();
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        return p.exitValue();
    }
}
