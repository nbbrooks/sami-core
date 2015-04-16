package sami.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.environment.EnvironmentListenerInt;
import sami.environment.EnvironmentProperties;
import sami.mission.ProjectListenerInt;
import sami.mission.ProjectSpecification;

/**
 *
 * @author pscerri
 */
public class Mediator {

    private static final Logger LOGGER = Logger.getLogger(Mediator.class.getName());

    public static final String LAST_DRM_FILE = "LAST_DRM_NAME";
    public static final String LAST_DRM_FOLDER = "LAST_DRM_FOLDER";
    public static final String LAST_EPF_FILE = "LAST_EPF_NAME";
    public static final String LAST_EPF_FOLDER = "LAST_EPF_FOLDER";

    // Environment related items
    private ProjectSpecification project = null;
    private File projectFile = null;
    private final ArrayList<ProjectListenerInt> projectListeners = new ArrayList<ProjectListenerInt>();
    // Environment related items
    private EnvironmentProperties environmentProperties = null;
    private File environmentPropertiesFile = null;
    private final ArrayList<EnvironmentListenerInt> environmentListeners = new ArrayList<EnvironmentListenerInt>();
//        private Platform platform = new Platform();
    // Variable name to number of references (for garbage collection)
    private HashMap<String, Integer> varToRefCount = new HashMap<String, Integer>();

    private static class MediatorHolder {

        public static final Mediator INSTANCE = new Mediator();
    }

    public static Mediator getInstance() {
        return MediatorHolder.INSTANCE;
    }

    private Mediator() {
    }

    public ProjectSpecification getProject() {
        if (project == null) {
            LOGGER.warning("Called getProject before loading/starting a DRM");
            boolean success;
            int answer;

            // Try to load the last used DRM file
            success = openLatestProject();
            if (!success) {
                LOGGER.severe("Failed to load last used DRM");
                answer = JOptionPane.showOptionDialog(null, "Failed to load last used DRM: Load different DRM or start new DRM?", "Load different DRM?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    success = openProjectFromBrowser();
                } else {
                    newProject();
                    success = true;
                }
            }
            while (!success) {
                LOGGER.severe("Failed to load specified DRM");
                answer = JOptionPane.showOptionDialog(null, "Failed to load specified DRM: Load different DRM or start new DRM?", "Load different DRM?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    success = openProjectFromBrowser();
                } else {
                    newProject();
                    success = true;
                }
            }
        }
        return project;
    }

    public File getProjectFile() {
        return projectFile;
    }

    public void newProject() {
        if (project != null && project.needsSaving()) {
            int answer = JOptionPane.showOptionDialog(null, "Save current specification?", "Save first?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                saveProject();
            }
        }
        // Set DRM location to NULL
        projectFile = null;
        // Create new DRM
        setProjectSpecification(new ProjectSpecification());
    }

    public void saveProject() {
        // If project has no location (IE is a new project), use save as instead
        if (projectFile == null) {
            saveProjectAs();
            return;
        }

        // Try to write the project to its location
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(projectFile));
            oos.writeObject(project);
            LOGGER.info("Writing DRM with " + project.getAllMissionPlans());
            project.saved();
            LOGGER.info("Saved: " + project);

            // Update latest DRM file and folder in Java preferences
            try {
                Preferences p = Preferences.userRoot();
                if (p == null) {
                    LOGGER.severe("Java preferences file is NULL");
                } else {
                    p.put(LAST_DRM_FILE, projectFile.getAbsolutePath());
                    p.put(LAST_DRM_FOLDER, projectFile.getParent());
                }
            } catch (AccessControlException e) {
                LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            }
        } catch (IOException ex) {
            LOGGER.severe("Failed to write DRM file: " + ex.toString());
        }
    }

    public void saveProjectAs() {
        JFileChooser chooser = new JFileChooser();
        // Try to set the chooser's directory to the last directory a DRM file was successfully loaded from
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_DRM_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last DRM folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last DRM folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        // Limit the chooser to .drm files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            // If file was manually named without DRM extension, add it
            if (chooser.getSelectedFile().getName().endsWith(".drm")) {
                projectFile = chooser.getSelectedFile();
            } else {
                projectFile = new File(chooser.getSelectedFile().getAbsolutePath() + ".drm");
            }
            // Save project to file
            LOGGER.info("Saving as: " + projectFile.toString());
            saveProject();
        }
        for (ProjectListenerInt listener : projectListeners) {
            listener.projectUpdated();
        }
    }

    /**
     * Attempts to load the last DRM file successfully opened as specified by
     * the user's Java preferences
     *
     * @return Success of loading last used DRM file
     */
    public boolean openLatestProject() {
        // Try to load the last used DRM file
        LOGGER.info("Load latest DRM");
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
                return false;
            } else {
                String lastDrmPath = p.get(LAST_DRM_FILE, null);
                if (lastDrmPath == null) {
                    LOGGER.warning("Last drm file preferences entry was NULL");
                    return false;
                }
                File lastDrmFile = new File(lastDrmPath);
                return openSpecifiedProject(lastDrmFile);
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            return false;
        }
    }

    /**
     * Prompts the user to specify a DRM file to attempt to load from the file
     * browser
     *
     * @return Whether a DRM file was successfully chosen and loaded
     */
    public boolean openProjectFromBrowser() {
        JFileChooser chooser = new JFileChooser();
        // Try to set the chooser's directory to the last directory a DRM file was successfully loaded from
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_DRM_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last DRM folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last DRM folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        // Limit the chooser to .drm files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            // Try to open the selected DRM file
            File selectedProjectFile = chooser.getSelectedFile();
            return openSpecifiedProject(selectedProjectFile);
        }
        return false;
    }

    public boolean openSpecifiedProject(File location) {
        if (location == null) {
            LOGGER.severe("Called openSpecifiedProject with NULL file");
            return false;
        }
        // Catch all exceptions
        try {
            LOGGER.info("Reading: " + location.toString());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(location));
            ProjectSpecification projectSpecTemp = (ProjectSpecification) ois.readObject();

            // Successfully loaded DRM file
            projectFile = location;
            setProjectSpecification(projectSpecTemp);
            // Update latest DRM file and folder in Java preferences
            try {
                Preferences p = Preferences.userRoot();
                if (p == null) {
                    LOGGER.severe("Java preferences file is NULL");
                } else {
                    p.put(LAST_DRM_FILE, location.getAbsolutePath());
                    p.put(LAST_DRM_FOLDER, location.getParent());
                }
            } catch (AccessControlException e) {
                LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            }
            return true;
        } catch (FileNotFoundException ex) {
            LOGGER.severe("Exception in DRM open - DRM file not found");
        } catch (InvalidClassException ex) {
            LOGGER.severe("Exception in DRM open - DRM version mismatch");
        } catch (StreamCorruptedException ex) {
            LOGGER.severe("Exception in DRM open - DRM version mismatch");
        } catch (SecurityException ex) {
            LOGGER.severe("Exception in DRM open - error in JDK SHA implementation");
        } catch (Exception ex) {
            LOGGER.severe("Exception in DRM open: " + ex.toString());
        }

        return false;
    }

    public void addProjectListener(ProjectListenerInt listener) {
        projectListeners.add(listener);
    }

    public void setProjectSpecification(ProjectSpecification projectSpec) {
        this.project = projectSpec;
        for (ProjectListenerInt listener : projectListeners) {
            listener.projectUpdated();
        }
    }

    public EnvironmentProperties getEnvironment() {
        if (environmentProperties == null) {
            LOGGER.warning("Called getEnvironment before loading/starting a EPF");
            boolean success;
            int answer;

            // Try to load the last used EPF file
            success = Mediator.getInstance().openLatestEnvironment();
            if (!success) {
                LOGGER.severe("Failed to load last used EPF");
                answer = JOptionPane.showOptionDialog(null, "Failed to load last used EPF: Load different EPF or start new EPF?", "Load different EPF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    success = Mediator.getInstance().openEnvironmentFromBrowser();
                } else {
                    newEnvironment();
                    success = true;
                }
            }
            while (!success) {
                LOGGER.severe("Failed to load specified EPF");
                answer = JOptionPane.showOptionDialog(null, "Failed to load specified EPF: Load different EPF or start new EPF?", "Load different EPF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (answer == JOptionPane.YES_OPTION) {
                    success = Mediator.getInstance().openEnvironmentFromBrowser();
                } else {
                    newEnvironment();
                    success = true;
                }
            }
        }

        return environmentProperties;
    }

    public File getEnvironmentFile() {
        return environmentPropertiesFile;
    }

    public void newEnvironment() {
        if (environmentProperties != null && environmentProperties.needsSaving()) {
            int answer = JOptionPane.showOptionDialog(null, "Save current specification?", "Save first?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                saveEnvironment();
            }
        }
        // Set EPF location to NULL
        environmentPropertiesFile = null;
        // Create new EPF
        setEnvironmentProperties(new EnvironmentProperties());
    }

    public void saveEnvironment() {
        // If environmentProperties has no location (IE is a new environmentProperties), use save as instead
        if (environmentPropertiesFile == null) {
            saveEnvironmentAs();
            return;
        }

        // Try to write the environmentProperties to its location
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(environmentPropertiesFile));
            oos.writeObject(environmentProperties);
            LOGGER.info("Writing EPF with " + environmentProperties);
            environmentProperties.saved();
            LOGGER.info("Saved: " + environmentProperties);

            // Update latest EPF file and folder in Java preferences
            try {
                Preferences p = Preferences.userRoot();
                if (p == null) {
                    LOGGER.severe("Java preferences file is NULL");
                } else {
                    p.put(LAST_EPF_FILE, environmentPropertiesFile.getAbsolutePath());
                    p.put(LAST_EPF_FOLDER, environmentPropertiesFile.getParent());
                }
            } catch (AccessControlException e) {
                LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            }
        } catch (IOException ex) {
            LOGGER.severe("Failed to write EPF file: " + ex.toString());
        }
    }

    public void saveEnvironmentAs() {
        JFileChooser chooser = new JFileChooser();
        // Try to set the chooser's directory to the last directory a EPF file was successfully loaded from
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_EPF_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last EPF folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last EPF folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        // Limit the chooser to .epf files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Environment properties files", "epf");
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            // If file was manually named without EPF extension, add it
            if (chooser.getSelectedFile().getName().endsWith(".epf")) {
                environmentPropertiesFile = chooser.getSelectedFile();
            } else {
                environmentPropertiesFile = new File(chooser.getSelectedFile().getAbsolutePath() + ".epf");
            }
            // Save environmentProperties to file
            LOGGER.info("Saving as: " + environmentPropertiesFile.toString());
            saveEnvironment();
        }
        for (EnvironmentListenerInt listener : environmentListeners) {
            listener.environmentUpdated();
        }
    }

    /**
     * Attempts to load the last EPF file successfully opened as specified by
     * the user's Java preferences
     *
     * @return Success of loading last used EPF file
     */
    public boolean openLatestEnvironment() {
        // Try to load the last used EPF file
        LOGGER.info("Load latest EPF");
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
                return false;
            } else {
                String lastEpfPath = p.get(LAST_EPF_FILE, null);
                if (lastEpfPath == null) {
                    LOGGER.warning("Last epf file preferences entry was NULL");
                    return false;
                }
                File lastEpfFile = new File(lastEpfPath);
                return openSpecifiedEnvironment(lastEpfFile);
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            return false;
        }
    }

    /**
     * Prompts the user to specify a EPF file to attempt to load from the file
     * browser
     *
     * @return Whether a EPF file was successfully chosen and loaded
     */
    public boolean openEnvironmentFromBrowser() {
        JFileChooser chooser = new JFileChooser();
        // Try to set the chooser's directory to the last directory a EPF file was successfully loaded from
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_EPF_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last EPF folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last EPF folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        // Limit the chooser to .epf files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Environment properties files", "epf");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            // Try to open the selected EPF file
            File selectedEnvironmentFile = chooser.getSelectedFile();
            return openSpecifiedEnvironment(selectedEnvironmentFile);
        }
        return false;
    }

    public boolean openSpecifiedEnvironment(File location) {
        if (location == null) {
            LOGGER.severe("Called openSpecifiedEnvironment with NULL file");
            return false;
        }
        // Catch all exceptions
        try {
            LOGGER.info("Reading: " + location.toString());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(location));
            EnvironmentProperties environmentPropertiesTemp = (EnvironmentProperties) ois.readObject();

            // Successfully loaded EPF file
            environmentPropertiesFile = location;
            setEnvironmentProperties(environmentPropertiesTemp);
            // Update latest EPF file and folder in Java preferences
            try {
                Preferences p = Preferences.userRoot();
                if (p == null) {
                    LOGGER.severe("Java preferences file is NULL");
                } else {
                    p.put(LAST_EPF_FILE, location.getAbsolutePath());
                    p.put(LAST_EPF_FOLDER, location.getParent());
                }
            } catch (AccessControlException e) {
                LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            }
            return true;
        } catch (FileNotFoundException ex) {
            LOGGER.severe("Exception in EPF open - EPF file not found");
        } catch (InvalidClassException ex) {
            LOGGER.severe("Exception in EPF open - EPF version mismatch");
        } catch (SecurityException ex) {
            LOGGER.severe("Exception in EPF open - error in JDK SHA implementation");
        } catch (Exception ex) {
            LOGGER.severe("Exception in EPF open: " + ex.toString());
        }

        return false;
    }

    public void setEnvironmentProperties(EnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
        for (EnvironmentListenerInt listener : environmentListeners) {
            listener.environmentUpdated();
        }
    }

    public void addEnvironmentListener(EnvironmentListenerInt listener) {
        environmentListeners.add(listener);
    }
}
