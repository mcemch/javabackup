/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javabackup.resources;

import java.util.*;

/**
 *
 * @author mce
 */
public class BackupThread implements Runnable {
        String dest_dir;
        ArrayList<String> src_dir_list;
        JavaBackup parent;

	public BackupThread(ArrayList<String> src_dir_list, String dest_dir, Object parent) {
	    this.src_dir_list = src_dir_list;
	    this.dest_dir = dest_dir;
	    this.parent = (JavaBackup) parent;
	}

	public void run() {
        // http://rosettacode.org/wiki/Execute_a_system_command
	    Date start_date = new Date();
        RsyncCommander rc = new RsyncCommander();
        int total = 0;
        this.parent.setProgress(0);
        this.parent.backupEnable(false);

        try {
            this.parent.textAreaAppend("Backup started: " + start_date.toString() + "\n\n");
            this.parent.setProgressText("Preparing...");

            for (String src_entry : src_dir_list) {
                this.parent.textAreaAppend("Preparing: " + src_entry + "\n");
                total += rc.rsyncGetTotal(src_entry, dest_dir, parent);
            }

            this.parent.setProgressTotal(total);
            this.parent.textAreaAppend("\n");
            this.parent.setProgressText("Syncing...");
            Thread.sleep(1000);

            for (String src_entry : src_dir_list) {
                this.parent.textAreaAppend("Backing up: " + src_entry + "\n");
                this.parent.incrementProgress();
                rc.rsync(src_entry, dest_dir, parent);

                // get ready for next entry
                if (this.parent.schedule.dry_run_mode) {
                    this.parent.textAreaAppend("- dry-run complete.\n");
                } else {
                    this.parent.textAreaAppend("- sync complete.\n");
                }
                this.parent.textAreaAppend("\n");
                this.parent.incrementProgress();
            }
            Thread.sleep(1000);

            if (this.parent.schedule.dry_run_mode) {
                this.parent.textAreaAppend("[Dry-Run Mode]: File system not modified.\n\n");
            }
        }
        catch (InterruptedException ex) {
        }

		this.parent.setProgressText("Complete.");
		Date stop_date = new Date();
		this.parent.textAreaAppend("Backup complete: " + stop_date.toString() + "\n");
		String elapsed_time = this.getElapsedTime(start_date, stop_date);
		this.parent.textAreaAppend("Elapsed time: " + elapsed_time + "\n\n");

        this.parent.backupEnable(true);

    }

	// get elapsed time given start and stop date objects
	public String getElapsedTime(Date start, Date stop) {
		StringBuilder elapsed_time = new StringBuilder();
		long duration = stop.getTime() - start.getTime();

		long days = duration / (1000 * 60 * 60 * 24);
		duration = duration % (1000 * 60 * 60 * 24);
		long hours = duration / (1000 * 60 * 60);
		duration = duration % (1000 * 60 * 60);
		long minutes = duration / (1000 * 60);
		duration = duration % (1000 * 60);
		long seconds = duration / 1000;

		elapsed_time.append(Long.toString(days) + " days, ");
		elapsed_time.append(Long.toString(hours) + " hrs, ");
		elapsed_time.append(Long.toString(minutes) + " mins, ");
		elapsed_time.append(Long.toString(seconds) + "secs.");

		return elapsed_time.toString();
	}
 
}
