/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package javabackup.resources;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 *
 * @author memch
 */
public class BackupSchedule {
        private static String homeDirectory = System.getProperty("user.home");
        private static final String javabackupDirectory = homeDirectory + "/.javabackup";                
	private static final String propertiesFilename = javabackupDirectory + "/JavaBackup.properties";
	private static final String scheduleDirectory = javabackupDirectory + "/schedule";
	private String scheduleFilename = "JavaBackup.xml";
	// current schedule data
	private String destinationDirectory;

	private TreeMap backupList;
	// options
	public Properties config;
	public boolean dry_run_mode;
	public boolean delete_mode;

	/*
	 * <!-- schedule file xml should be of the form -->
	 * <schedule>
	 *     <destDir>/some/location</destDir>
	 *     <backupList>
	 *         <backupEntry name="/some/location/1" value="1"/>
	 *         <backupEntry name="/some/location/2" value="1"/>
	 *         <!-- ... -->
	 *     </backupList>
	 * </schedule>
	 */

	public BackupSchedule() {
		config = new Properties();
		backupList = null;
		File schedule_directory = new File(scheduleDirectory);
                File backup_directory = new File(javabackupDirectory);
                
                
                if (!backup_directory.exists()) {
                    backup_directory.mkdir();
                }
		if (!schedule_directory.exists()) {
			schedule_directory.mkdir();
		}
		File schedule_file = new File(schedule_directory + "/" + scheduleFilename);
		if (!schedule_file.exists()) {
			this.writeDefaultSchedule();
		}
		this.readProperties();
		this.readSchedule();
	}


	// read the properties file
	public void readProperties() {
	    File propertiesFile = new File(this.propertiesFilename);
	    if(propertiesFile.exists()) {
            config = PropertyLoader.loadProperties(this.propertiesFilename);
            scheduleFilename = config.getProperty("CURRENT_SCHEDULE");
            String test = config.getProperty("DRY_RUN_MODE");
            if(config.getProperty("DRY_RUN_MODE") != null) {
                if (config.getProperty("DRY_RUN_MODE").toLowerCase().matches("true")) {
                    dry_run_mode =  true;

                } else { 
                    dry_run_mode = false;
                } 
            }
            else {
                config.setProperty("DRY_RUN_MODE", "true" );
                dry_run_mode = true;
            }

            if(config.getProperty("DELETE_MODE") != null) {
                if(config.getProperty("DELETE_MODE").toLowerCase().matches("true")) {
                    delete_mode = true;
                } else { 
                    delete_mode = false; 
                }
            }
	    }
	    else {
            scheduleFilename = "JavaBackup.xml";
            dry_run_mode = true;
            delete_mode = false;
            config.setProperty("CURRENT_SCHEDULE", scheduleFilename);
            config.setProperty("DRY_RUN_MODE", "true" );
            config.setProperty("DELETE_MODE", "false");
            this.writeProperties();
	    }
	}


	// write the properties file
	public void writeProperties() {
	    try {
            Date date = new Date();
            FileOutputStream out = new FileOutputStream(propertiesFilename);
            config.store(out, date.toString());
            out.close();
	    }
	    catch(IOException ex) {
            ex.printStackTrace();
	    }
	}

	public void loadSchedule(String filename) {
		String orig_schedule_filename = scheduleFilename;
		scheduleFilename = filename;
		readSchedule();
		scheduleFilename = orig_schedule_filename;
	}


	// read the schedule file
	public void readSchedule() {
	    // http://www.java-tips.org/java-se-tips/javax.xml.parsers/how-to-read-xml-file-in-java.html 
	    // http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
	    // http://www.java-samples.com/showtutorial.php?tutorialid=152
	    // http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
        // http://www.javapractices.com/topic/TopicAction.do?Id=42

	    File schedule_xml_file = new File(scheduleDirectory + "/" + scheduleFilename);
        String schedule_xml_contents = new String();

	    if (!schedule_xml_file.exists()) {
		    System.out.println("No schedule file: " + scheduleDirectory + "/" + scheduleFilename);
                    this.writeDefaultSchedule();
	    }

        String schedule_xml_abs_path = schedule_xml_file.getAbsolutePath();

	    try {
            Scanner scanner = new Scanner(new FileInputStream(schedule_xml_abs_path));
            // if the file is empty, create a new one
            if (scanner.hasNextLine() == false) {
                this.writeDefaultSchedule();
            }

	    }
	    catch (IOException ex) {
		    ex.printStackTrace();
	    }


	    this.backupList = new TreeMap<String, Boolean>();

	    try {
            DocumentBuilderFactory doc_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder doc_builder = doc_factory.newDocumentBuilder();
            Document doc = doc_builder.parse(schedule_xml_file);

            doc.getDocumentElement().normalize();

            NodeList destDirNodeList = doc.getElementsByTagName("destDir");

            if (destDirNodeList.getLength() != 1) {
                // do some error thing
            } else {
                Node destDirNode = destDirNodeList.item(0);
                if (destDirNode.hasChildNodes()) {
                    this.destinationDirectory = destDirNode.getFirstChild().getNodeValue();
                }
            }


            NodeList backupDirNodeList = doc.getElementsByTagName("backupList");

            if (backupDirNodeList.getLength() != 1) {
                // do some error
            } else {
                Element backupDirElement = (Element) backupDirNodeList.item(0);
                NodeList backupDirEntryList = backupDirElement.getElementsByTagName("backupEntry");
                for (int i=0; i < backupDirEntryList.getLength(); i++) {
                    Node backupDirEntryNode = backupDirEntryList.item(i);
                    Element backupDirEntry = (Element) backupDirEntryNode;
                    String backupEntry  = backupDirEntry.getAttribute("name");
                    String backupEntryValue = backupDirEntry.getAttribute("value");
                    boolean backupEntryChecked;
                    if (backupEntryValue.matches("1")) {
                        backupEntryChecked = true;
                    } else {
                        backupEntryChecked = false;
                    }
                    this.addBackupList(backupEntry,backupEntryChecked);
                }
            }

	    }
	    catch (ParserConfigurationException ex) {
            ex.printStackTrace();
	    }
        catch (SAXParseException ex) {
	        ex.printStackTrace();
	    }
	    catch (SAXException ex) {
            ex.printStackTrace();
	    }
	    catch (IOException ex) {
            ex.printStackTrace();
	    }

	}

	// initialize the scheduleFile
	public void writeDefaultSchedule() {
		try {
		    BufferedWriter out = new BufferedWriter(new FileWriter(scheduleDirectory + "/" + scheduleFilename));
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		    out.write("<schedule>\n");
            out.write("<destDir></destDir>\n");
            out.write("<backupList>\n");
		    out.write("</backupList>\n");
		    out.write("</schedule>\n");
		    out.flush();
		    out.close();

		} catch (IOException e) {
		    e.printStackTrace();
		}
	}


	// write the schedule file
	public void writeSchedule() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(scheduleDirectory + "/" + scheduleFilename));
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
            out.write("<schedule>\n");
            if (destinationDirectory != null) {
                out.write("<destDir>" + destinationDirectory + "</destDir>\n");
            } else {
                out.write("<destDir/>\n");
            }
            out.write("<backupList>\n");
            // get all entries in the list
            Iterator it = backupList.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<String, Boolean> entry = (Map.Entry) it.next();
                String list_item = new String((String)entry.getKey());
                String list_value = new String();
                if ((boolean)entry.getValue() == true ) {
                    list_value = "1";
                } else {
                    list_value = "0";
                }
                out.write("<backupEntry name=\"" + list_item + "\" value=\"" + list_value + "\"/>\n");
            }
            out.write("</backupList>\n");
            out.write("</schedule>\n");
            out.flush();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
	}


	// save the schedule as filename
	public void saveScheduleAs(String filename) {
		String defaultFilename = this.scheduleFilename;
		this.scheduleFilename = filename;
		this.writeSchedule();

	}


	// set current schedule destination directory DOS Path
	public void setDestinationDirectory(String dir) {
		destinationDirectory = dir;
                this.writeSchedule();
	}

	// get current schedule destination directory DOS Path
	public String getDestinationDirectory() {
		return destinationDirectory;
	}

	// convert current schedule destination direcotry DOS Path to CygPath
	public String getDestinationDirectoryCygPath() {
		String destinationDirectoryCygPath = new String();

		if (destinationDirectory != null) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                String temp_path = destinationDirectory.replaceAll("\\\\", "/");
                destinationDirectoryCygPath =
                temp_path.replaceAll("^([A-Z]):/(.*)$", "/cygdrive/$1/$2" );
            } else {
                destinationDirectoryCygPath = destinationDirectory;
            }
        }

		return destinationDirectoryCygPath;
	}


	// add directory to backup list
	public void addBackupList(String item, boolean checked) {
		backupList.put(item, checked);
	}

	// remove directory from backup list
	public void removeBackupList(String item) {
		if (backupList.containsKey(item)) {
			backupList.remove(item);
		}
	}

	// get the entire backup list map
	public TreeMap<String, Boolean> getBackupList() {
		return backupList;
	}


	// get the list of directories to backup in CygPath format
	public ArrayList<String> getBackupListCygPath() {
	    ArrayList<String> cygpath_list = null ;

	    if (backupList != null) {
		    cygpath_list = new ArrayList<String>();
		    Iterator it = backupList.entrySet().iterator();
		    while(it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			if ((Boolean)entry.getValue()) {
				String list_item = new String((String)entry.getKey());
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    list_item = list_item.replaceAll("\\\\", "/");
				    list_item = list_item.replaceAll("^([A-Z]):/(.*)$",
                        "/cygdrive/$1/$2");
                                }
                    cygpath_list.add(list_item);
                }
		    }
	    }
	    return cygpath_list;
	}

}
