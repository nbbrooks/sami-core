package sami.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
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
    private ProjectSpecification projectSpec = null;
    private File projectSpecLocation = null;
    private final ArrayList<ProjectListenerInt> projectListeners = new ArrayList<ProjectListenerInt>();
    // Environment related items
    private EnvironmentProperties environmentProperties = null;
    private File environmentPropertiesLocation = null;
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
        if (projectSpec == null) {
            newProject();
        }

        return projectSpec;
    }

    public File getProjectFile() {
        return projectSpecLocation;
    }

    public void newProject() {
        if (projectSpec != null && projectSpec.needsSaving()) {
            int answer = JOptionPane.showOptionDialog(null, "Save current specification?", "Save first?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                saveProject();
            }
        }
        projectSpecLocation = null;
        setProjectSpecification(new ProjectSpecification());
    }

    public void saveProject() {
        if (projectSpecLocation == null) {
            saveProjectAs();
            if (projectSpecLocation == null) {
                return;
            }
        }
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(projectSpecLocation));
            oos.writeObject(projectSpec);
            LOGGER.info("Writing projectSpec with " + projectSpec.getAllMissionPlans());
            projectSpec.saved();
            LOGGER.info("Saved: " + projectSpec);

            // Update last DRM file
            Preferences p = Preferences.userRoot();
            try {
                p.put(LAST_DRM_FILE, projectSpecLocation.getAbsolutePath());
                p.put(LAST_DRM_FOLDER, projectSpecLocation.getParent());
            } catch (AccessControlException e) {
                LOGGER.severe("Failed to save preferences");
            }
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveProjectAs() {
        Preferences p = Preferences.userRoot();
        String folder = p.get(LAST_DRM_FOLDER, "");
        JFileChooser chooser = new JFileChooser(folder);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().getName().endsWith(".drm")) {
                projectSpecLocation = chooser.getSelectedFile();
            } else {
                projectSpecLocation = new File(chooser.getSelectedFile().getAbsolutePath() + ".drm");
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Saving as: " + projectSpecLocation.toString());
            saveProject();
        }
    }

    public boolean openProject() {
        Preferences p = Preferences.userRoot();
        String folder = p.get(LAST_DRM_FOLDER, "");
        JFileChooser chooser = new JFileChooser(folder);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            projectSpecLocation = chooser.getSelectedFile();
            return openProject(projectSpecLocation);
        }
        return false;
    }

    public boolean openProject(File location) {
        if (location == null) {
            return false;
        }
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Reading: " + location.toString());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(location));
            ProjectSpecification projectSpecTemp = (ProjectSpecification) ois.readObject();

            if (projectSpecTemp == null) {
                return false;
            } else {
                projectSpecLocation = location;
                setProjectSpecification(projectSpecTemp);
                Preferences p = Preferences.userRoot();
                try {
                    p.put(LAST_DRM_FILE, location.getAbsolutePath());
                    p.put(LAST_DRM_FOLDER, location.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }
                return true;
            }
        } catch (FileNotFoundException ex) {
            LOGGER.severe("Exception in DRM open - DRM file not found");
        } catch (InvalidClassException ex) {
            LOGGER.severe("Exception in DRM open - DRM version mismatch");
        } catch (SecurityException ex) {
            LOGGER.severe("Exception in DRM open - error in JDK SHA implementation");
        } catch (Exception ex) {
            LOGGER.severe("Exception in DRM open: " + ex.getLocalizedMessage());
        }

        return false;
    }

    public void addProjectListener(ProjectListenerInt listener) {
        projectListeners.add(listener);
    }

    public void setProjectSpecification(ProjectSpecification projectSpec) {
        this.projectSpec = projectSpec;
        for (ProjectListenerInt listener : projectListeners) {
            listener.projectUpdated();
        }
    }

    public EnvironmentProperties getEnvironment() {
        if (environmentProperties == null) {
            newEnvironment();
        }

        return environmentProperties;
    }

    public File getEnvironmentFile() {
        return environmentPropertiesLocation;
    }

    public void newEnvironment() {
        if (environmentProperties != null && environmentProperties.needsSaving()) {
            int answer = JOptionPane.showOptionDialog(null, "Save current environment?", "Save first?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                saveEnvironment();
            }
        }
        environmentPropertiesLocation = null;
        setEnvironmentProperties(new EnvironmentProperties());
    }

    public void saveEnvironment() {
        if (environmentPropertiesLocation == null) {
            saveEnvironmentAs();
            if (environmentPropertiesLocation == null) {
                return;
            }
        }
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(environmentPropertiesLocation));
            oos.writeObject(environmentProperties);
            LOGGER.info("Writing environmentProperties with " + environmentProperties);
            environmentProperties.saved();
            LOGGER.info("Saved: " + environmentProperties);

            // Update last DRM file
            Preferences p = Preferences.userRoot();
            try {
                p.put(LAST_EPF_FILE, environmentPropertiesLocation.getAbsolutePath());
                p.put(LAST_EPF_FOLDER, environmentPropertiesLocation.getParent());
            } catch (AccessControlException e) {
                LOGGER.severe("Failed to save preferences");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void saveEnvironmentAs() {
        Preferences p = Preferences.userRoot();
        String folder = p.get(LAST_EPF_FOLDER, "");
        JFileChooser chooser = new JFileChooser(folder);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Environment Properties files", "epf");
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().getName().endsWith(".epf")) {
                environmentPropertiesLocation = chooser.getSelectedFile();
            } else {
                environmentPropertiesLocation = new File(chooser.getSelectedFile().getAbsolutePath() + ".drm");
            }
            LOGGER.info("Saving as: " + environmentPropertiesLocation.toString());
            saveEnvironment();
        }
    }

    public boolean openEnvironment() {
        Preferences p = Preferences.userRoot();
        String folder = p.get(LAST_EPF_FOLDER, "");
        JFileChooser chooser = new JFileChooser(folder);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Environment Properties files", "epf");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            environmentPropertiesLocation = chooser.getSelectedFile();
            return openEnvironment(environmentPropertiesLocation);
        }
        return false;
    }

    public boolean openEnvironment(File location) {
        if (location == null) {
            return false;
        }
        try {
            LOGGER.info("Reading: " + location.toString());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(location));
            EnvironmentProperties environmentPropertiesTemp = (EnvironmentProperties) ois.readObject();

            if (environmentPropertiesTemp == null) {
                return false;
            } else {
                environmentPropertiesLocation = location;
                setEnvironmentProperties(environmentPropertiesTemp);
                Preferences p = Preferences.userRoot();
                try {
                    p.put(LAST_EPF_FILE, location.getAbsolutePath());
                    p.put(LAST_EPF_FOLDER, location.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }
                return true;
            }
        } catch (FileNotFoundException ex) {
            LOGGER.severe("Exception in EPF open - EPF file not found");
        } catch (InvalidClassException ex) {
            LOGGER.severe("Exception in EPF open - EPF version mismatch");
        } catch (SecurityException ex) {
            LOGGER.severe("Exception in EPF open - error in JDK SHA implementation");
        } catch (Exception ex) {
            LOGGER.severe("Exception in EPF open: " + ex.getLocalizedMessage());
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
