package sami.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AccessControlException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DomainConfigManager {

    private static final Logger LOGGER = Logger.getLogger(DomainConfigManager.class.getName());
    private static volatile DomainConfigManager instance = null;
    public DomainConfig domainConfiguration = null;

    private DomainConfigManager() {
        // Try to load the last dcf automatically
        Preferences p = Preferences.userRoot();
        String lastConfName = p.get(DomainConfigF.LAST_DCF_FILE, null);
        if (lastConfName != null) {
            domainConfiguration = load(lastConfName);
        }
        // If it fails, have the user specify a dcf
        if (domainConfiguration == null || !domainConfiguration.complete) {
            JOptionPane.showMessageDialog(null, "Could not load last used domain configuration (.DCF), please specify file");
            String path = getPath();
            domainConfiguration = load(path);
        }
        // If it fails/user cancels, exit
        if (domainConfiguration == null || !domainConfiguration.complete) {
            System.exit(0);
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

    public String getPath() {
        Preferences p = Preferences.userRoot();
        String lastConfName = p.get(DomainConfigF.LAST_DCF_FOLDER, "");
        JFileChooser chooser = new JFileChooser(lastConfName);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Domain configuration files", "dcf");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile().getAbsolutePath();
    }

    public DomainConfig load(String path) {
        if (path == null) {
            return null;
        }
        DomainConfig dc = null;
        File file = new File(path);
        LOGGER.info("Loading .DCF: " + file.toString());
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            dc = (DomainConfig) ois.readObject();
            dc.reload();
            if (!dc.complete) {
                LOGGER.severe("Loaded .DCF is not complete: " + path);
                return null;
            }
            LOGGER.info(dc.toVerboseString());

            // Update Preferences
            try {
                Preferences p = Preferences.userRoot();
                p.put(DomainConfigF.LAST_DCF_FILE, file.getAbsolutePath());
                p.put(DomainConfigF.LAST_DCF_FOLDER, file.getParent());
            } catch (AccessControlException e) {
                LOGGER.severe("Failed to save preferences");
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (IOException ioe) {
            LOGGER.warning("Railed to load DomainConfiguration");
        }
        return dc;
    }
}
