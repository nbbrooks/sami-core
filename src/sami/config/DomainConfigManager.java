package sami.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.security.AccessControlException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DomainConfigManager {

    private static final Logger LOGGER = Logger.getLogger(DomainConfigManager.class.getName());

    public static final String LAST_DCF_FILE = "LAST_DREAAM_DCF_NAME";
    public static final String LAST_DCF_FOLDER = "LAST_DREAAM_DCF_FOLDER";
    public static final String LAST_CFG_FOLDER = "LAST_DREAAM_CFG_FOLDER";
    private static volatile DomainConfigManager instance = null;
    private DomainConfig domainConfiguration = null;
    private File domainConfigurationLocation = null;

    private DomainConfigManager() {
        // Try to load the last dcf automatically
        Preferences p = Preferences.userRoot();
        try {
            String lastDcfPath = p.get(LAST_DCF_FILE, null);
            LOGGER.info("Load last DCF: " + lastDcfPath);
            if (lastDcfPath != null) {
                boolean success = openDomainConfiguration(new File(lastDcfPath));
                if (!success) {
                    // Failed - create new one
                    LOGGER.info("Couldn't lost last DCF, creating new one");
                    newDomainConfiguration();
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to load last used DCF");
        }
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

    public void newDomainConfiguration() {
        //@todo check if changes need saving in currently open file
        domainConfigurationLocation = null;
        domainConfiguration = new DomainConfig();
    }

    public DomainConfig getDomainConfiguration() {
        return domainConfiguration;
    }

    public File getDomainConfigurationFile() {
        return domainConfigurationLocation;
    }

    public boolean openDomainConfiguration() {
        Preferences p = Preferences.userRoot();
        String folder = p.get(LAST_DCF_FOLDER, "");
        JFileChooser chooser = new JFileChooser(folder);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Domain configuration files", "dcf");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            domainConfigurationLocation = chooser.getSelectedFile();
            return openDomainConfiguration(domainConfigurationLocation);
        }
        return false;
    }

    public boolean openDomainConfiguration(File location) {
        if (location == null) {
            LOGGER.warning("Tried to open NULL file as DCF");
            return false;
        }
        try {
            LOGGER.info("Reading: " + location.toString());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(location));
            DomainConfig domainConfigurationTemp = (DomainConfig) ois.readObject();

            if (domainConfigurationTemp == null) {
                return false;
            } else {
                domainConfigurationTemp.reload();

                if (!domainConfigurationTemp.complete) {
                    LOGGER.severe("Loaded .DCF is not complete: " + location.getAbsolutePath());
                    return false;
                }

                // Success
                domainConfigurationLocation = location;
                domainConfiguration = domainConfigurationTemp;
                Preferences p = Preferences.userRoot();
                try {
                    p.put(LAST_DCF_FILE, location.getAbsolutePath());
                    p.put(LAST_DCF_FOLDER, location.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }

                return true;
            }
        } catch (FileNotFoundException ex) {
            LOGGER.severe("Exception in DCF open - DCF file not found");
        } catch (InvalidClassException ex) {
            LOGGER.severe("Exception in DCF open - DCF version mismatch");
        } catch (SecurityException ex) {
            LOGGER.severe("Exception in DCF open - error in JDK SHA implementation");
        } catch (Exception ex) {
            LOGGER.severe("Exception in DCF open: " + ex.getLocalizedMessage());
        }

        return false;
    }
}
