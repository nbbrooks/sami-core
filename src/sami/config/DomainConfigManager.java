package sami.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessControlException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DomainConfigManager {

    private static final Logger LOGGER = Logger.getLogger(DomainConfigManager.class.getName());

    public static final String LAST_DCF_FILE = "LAST_DREAAM_DCF_NAME_CRW";
    public static final String LAST_DCF_FOLDER = "LAST_DREAAM_DCF_FOLDER_CRW";
    public static final String LAST_CFG_FOLDER = "LAST_DREAAM_CFG_FOLDER_CRW";
    private static volatile DomainConfigManager instance = null;
    private DomainConfig domainConfig = null;
    private File domainConfigFile = null;

    private DomainConfigManager() {
    }

    public static DomainConfigManager getInstance() {
        if (instance == null) {
            synchronized (DomainConfigManager.class) {
                if (instance == null) {
                    instance = new DomainConfigManager();
                }
            }
        }
        return instance;
    }

    public DomainConfig getDomainConfiguration() {
        if (domainConfig == null) {
            LOGGER.warning("Called getDomainConfiguration before loading/starting a DCF");
            boolean success;
            int answer;
            // Try to load the last used DCF file
            success = openLatestDomainConfiguration();
            if (!success) {
                LOGGER.severe("Failed to load last used DCF");
                answer = JOptionPane.showOptionDialog(null, "Failed to load last used DCF: Load different DCF or start new DCF?", "Load different DCF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    success = openDomainConfigurationFromBrowser();
                } else {
                    newDomainConfiguration();
                    success = true;
                }
            }
            while (!success) {
                LOGGER.severe("Failed to load specified DCF");
                answer = JOptionPane.showOptionDialog(null, "Failed to load specified DCF: Load different DCF or start new DCF?", "Load different DCF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    success = openDomainConfigurationFromBrowser();
                } else {
                    newDomainConfiguration();
                    success = true;
                }
            }
        }
        return domainConfig;
    }

    public File getDomainConfigurationFile() {
        return domainConfigFile;
    }

    public void newDomainConfiguration() {
        // Set DCF location to NULL
        domainConfigFile = null;
        // Create new DCF
        domainConfig = new DomainConfig();
    }

    public void saveDomainConfiguration() {
        // If domainConfig has no location (IE is a new domainConfig), use save as instead
        if (domainConfigFile == null) {
            saveDomainConfigurationAs();
            return;
        }

        // Try to write the domainConfig to its location
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(domainConfigFile));
            oos.writeObject(domainConfig);
            LOGGER.info("Writing DCF with " + domainConfig);
            LOGGER.info("Saved: " + domainConfig);

            // Update latest DCF file and folder in Java preferences
            try {
                Preferences p = Preferences.userRoot();
                if (p == null) {
                    LOGGER.severe("Java preferences file is NULL");
                } else {
                    p.put(LAST_DCF_FILE, domainConfigFile.getAbsolutePath());
                    p.put(LAST_DCF_FOLDER, domainConfigFile.getParent());
                }
            } catch (AccessControlException e) {
                LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            }
        } catch (IOException ex) {
            LOGGER.severe("Failed to write DCF file: " + ex.toString());
        }
    }

    public void saveDomainConfigurationAs() {
        JFileChooser chooser = new JFileChooser();
        // Try to set the chooser's directory to the last directory a DCF file was successfully loaded from
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_DCF_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last DCF folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last DCF folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        // Limit the chooser to .dcf files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Domain configuration files", "dcf");
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            // If file was manually named without DCF extension, add it
            if (chooser.getSelectedFile().getName().endsWith(".dcf")) {
                domainConfigFile = chooser.getSelectedFile();
            } else {
                domainConfigFile = new File(chooser.getSelectedFile().getAbsolutePath() + ".dcf");
            }
            // Save domainConfig to file
            LOGGER.info("Saving as: " + domainConfigFile.toString());
            saveDomainConfiguration();
        }
    }

    /**
     * Attempts to load the last DCF file successfully opened as specified by
     * the user's Java preferences
     *
     * @return Success of loading last used DCF file
     */
    public boolean openLatestDomainConfiguration() {
        // Try to load the last used DCF file
        LOGGER.info("Load latest DCF");
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
                return false;
            } else {
                String lastDcfPath = p.get(LAST_DCF_FILE, null);
                if (lastDcfPath == null) {
                    LOGGER.warning("Last dcf file preferences entry was NULL");
                    return false;
                }
                File lastDcfFile = new File(lastDcfPath);
                return openSpecifiedDomainConfiguration(lastDcfFile);
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            return false;
        }
    }

    /**
     * Prompts the user to specify a DCF file to attempt to load from the file
     * browser
     *
     *
     * @return Whether a DCF file was successfully chosen and loaded
     */
    public boolean openDomainConfigurationFromBrowser() {
        JFileChooser chooser = new JFileChooser();
        // Try to set the chooser's directory to the last directory a DCF file was successfully loaded from
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_DCF_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last DCF folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last DCF folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        // Limit the chooser to .dcf files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Domain configuration files", "dcf");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            // Try to open the selected DCF file
            File selectedDomainConfigurationFile = chooser.getSelectedFile();
            return openSpecifiedDomainConfiguration(selectedDomainConfigurationFile);
        }
        return false;
    }

    public boolean openSpecifiedDomainConfiguration(File location) {
        if (location == null) {
            LOGGER.severe("Called openSpecifiedDomainConfiguration with NULL file");
            return false;
        }
        // Catch all exceptions
        try {
            LOGGER.info("Reading: " + location.toString());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(location));
            DomainConfig domainConfigTemp = (DomainConfig) ois.readObject();

            // Check if .cfg files are present on this system and update DCF
            domainConfigTemp.loadCfgValues();
            if (!domainConfigTemp.complete) {
                // Both .cfg file and saved entry are missing for one/more config fields
                LOGGER.warning("Loaded DCF is not complete: " + location.getAbsolutePath());
            }

            // Successfully loaded DCF file
            domainConfigFile = location;
            domainConfig = domainConfigTemp;
            // Update latest DCF file and folder in Java preferences
            try {
                Preferences p = Preferences.userRoot();
                if (p == null) {
                    LOGGER.severe("Java preferences file is NULL");
                } else {
                    p.put(LAST_DCF_FILE, location.getAbsolutePath());
                    p.put(LAST_DCF_FOLDER, location.getParent());
                }
            } catch (AccessControlException e) {
                LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            }
            return true;
        } catch (FileNotFoundException ex) {
            LOGGER.severe("Exception in DCF open - DCF file not found");
        } catch (InvalidClassException ex) {
            LOGGER.severe("Exception in DCF open - DCF version mismatch");
        } catch (SecurityException ex) {
            LOGGER.severe("Exception in DCF open - error in JDK SHA implementation");
        } catch (Exception ex) {
            LOGGER.severe("Exception in DCF open: " + ex.toString());
        }

        return false;
    }
}
