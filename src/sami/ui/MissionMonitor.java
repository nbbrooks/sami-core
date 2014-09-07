package sami.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.config.DomainConfigManager;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.environment.EnvironmentProperties;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.ProjectSpecification;
import sami.service.ServiceServer;
import sami.uilanguage.LocalUiClientServer;
import sami.uilanguage.UiFrame;

/**
 *
 * @author pscerri
 */
public class MissionMonitor extends javax.swing.JFrame implements PlanManagerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(MissionMonitor.class.getName());
    public static final String LAST_DRM_FILE = "LAST_DRM_NAME";
    public static final String LAST_DRM_FOLDER = "LAST_DRM_FOLDER";
    public static final String LAST_EPF_FILE = "LAST_EPF_NAME";
    public static final String LAST_EPF_FOLDER = "LAST_EPF_FOLDER";
    DefaultListModel missionListModel = new DefaultListModel();
    ArrayList<Object> uiElements = new ArrayList<Object>();
    ArrayList<UiFrame> uiFrames = new ArrayList<UiFrame>();
    HashMap<PlanManager, MissionDisplay> pmToDisplay = new HashMap<PlanManager, MissionDisplay>();
    private ArrayList<MissionDisplay> missionDisplayList = new ArrayList<MissionDisplay>();

    /**
     * Creates new form MissionMonitor
     */
    public MissionMonitor() {
        LOGGER.info("java.version: " + System.getProperty("java.version"));
        LOGGER.info("sun.arch.data.model: " + System.getProperty("sun.arch.data.model"));
        LOGGER.info("java.class.path: " + System.getProperty("java.class.path"));
        LOGGER.info("java.library.path: " + System.getProperty("java.library.path"));
        LOGGER.info("java.ext.dirs: " + System.getProperty("java.ext.dirs"));
        LOGGER.info("java.util.logging.config.file: " + System.getProperty("java.util.logging.config.file"));
        LOGGER.info("domainConfiguration:\n" + DomainConfigManager.getInstance().domainConfiguration.toString());
        LOGGER.info("domainConfiguration:\n" + DomainConfigManager.getInstance().domainConfiguration.toVerboseString());

        initComponents();

        (new FrameManager()).setVisible(true);

//        InformationGenerationFrame igf = new InformationGenerationFrame();
//        igf.setVisible(true);
        LocalUiClientServer clientServer = new LocalUiClientServer();
        Engine.getInstance().setUiClient(clientServer);
        Engine.getInstance().setUiServer(clientServer);
        // Set Engine singleton's services server
        Engine.getInstance().setServiceServer(new ServiceServer());

        for (String className : DomainConfigManager.getInstance().domainConfiguration.uiList) {
            try {
                LOGGER.info("Initializing UI class: " + className);
                Class uiClass = Class.forName(className);
                Object uiElement = uiClass.getConstructor(new Class[]{}).newInstance();
                if (uiElement instanceof UiFrame) {
                    uiFrames.add((UiFrame) uiElement);
                }
                uiElements.add(uiElement);
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            } catch (InstantiationException ie) {
                ie.printStackTrace();
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            } catch (NoSuchMethodException nsme) {
                nsme.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
                System.out.println(ite.getCause());
            }
        }

//        new StateManagerAccessor();
        missionViewersP.setLayout(new BoxLayout(missionViewersP, BoxLayout.Y_AXIS));
        planL.setModel(missionListModel);

//        (new AgentPlatform()).showMonitor();
        FrameManager.restoreLayout();

        Engine.getInstance().addListener(this);

        // Try to load the last used DRM file
        Preferences p = Preferences.userRoot();
        try {
            String lastDrmPath = p.get(LAST_DRM_FILE, null);
            if (lastDrmPath != null) {
                loadDrm(new File(lastDrmPath));
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to load last used DRM");
        }
        // Try to load the last used EPF file
        try {
            String lastEpfPath = p.get(LAST_EPF_FILE, null);
            if (lastEpfPath != null) {
                loadEpf(new File(lastEpfPath));
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to load last used EPF");
        }
    }

    public HashMap<String, String> loadUi(String uiF) {
        HashMap<String, String> uiElements = new HashMap<String, String>();
        String uiClass, uiDescription;
        Pattern pattern = Pattern.compile("\"[A-Za-z0-9\\.]+\"\\s+\"[^\r\n\"]*\"\\s*");
        try {
            FileInputStream fstream = new FileInputStream(uiF);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.matches(pattern.toString())) {
                    line = line.trim();
                    String[] pairing = splitOnString(line, "\"");
                    if (pairing.length == 4) {
                        uiClass = pairing[1];
                        uiDescription = pairing[3];
                        uiElements.put(uiClass, uiDescription);
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return uiElements;
    }

    private String[] splitOnString(String string, String split) {
        ArrayList<String> list = new ArrayList<String>();
        int startIndex = 0;
        int endIndex = string.indexOf(split, startIndex);
        while (endIndex != -1) {
            list.add(string.substring(startIndex, endIndex));
            startIndex = endIndex + 1;
            endIndex = string.indexOf(split, startIndex);
        }
        String[] ret = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ret[i] = list.get(i);
        }
        return ret;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        loadDrmB = new javax.swing.JButton();
        runB = new javax.swing.JButton();
        planScrollP = new javax.swing.JScrollPane();
        planL = new javax.swing.JList();
        missionsScrollP = new javax.swing.JScrollPane();
        placeholderP = new javax.swing.JPanel();
        missionViewersP = new javax.swing.JPanel();
        loadEpfB = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Mission Monitor");

        loadDrmB.setText("Load Project");
        loadDrmB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadDrmBActionPerformed(evt);
            }
        });

        runB.setText("Run");
        runB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runBActionPerformed(evt);
            }
        });

        planL.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { " " };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        planScrollP.setViewportView(planL);

        missionsScrollP.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        org.jdesktop.layout.GroupLayout missionViewersPLayout = new org.jdesktop.layout.GroupLayout(missionViewersP);
        missionViewersP.setLayout(missionViewersPLayout);
        missionViewersPLayout.setHorizontalGroup(
            missionViewersPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 799, Short.MAX_VALUE)
        );
        missionViewersPLayout.setVerticalGroup(
            missionViewersPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 458, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout placeholderPLayout = new org.jdesktop.layout.GroupLayout(placeholderP);
        placeholderP.setLayout(placeholderPLayout);
        placeholderPLayout.setHorizontalGroup(
            placeholderPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(placeholderPLayout.createSequentialGroup()
                .add(missionViewersP, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        placeholderPLayout.setVerticalGroup(
            placeholderPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, placeholderPLayout.createSequentialGroup()
                .addContainerGap()
                .add(missionViewersP, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        missionsScrollP.setViewportView(placeholderP);

        loadEpfB.setText("Load Environment");
        loadEpfB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadEpfBActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(planScrollP, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                    .add(runB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(loadDrmB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(loadEpfB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(missionsScrollP)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(missionsScrollP)
                    .add(layout.createSequentialGroup()
                        .add(runB)
                        .add(5, 5, 5)
                        .add(planScrollP)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loadDrmB)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loadEpfB)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loadDrmBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDrmBActionPerformed
        File specLocation = null;

        if (specLocation == null) {
            Preferences p = Preferences.userRoot();
            String lastDrmFolder = p.get(LAST_DRM_FOLDER, "");
            JFileChooser chooser = new JFileChooser(lastDrmFolder);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
            chooser.setFileFilter(filter);
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION) {
                specLocation = chooser.getSelectedFile();
            }
        }
        loadDrm(specLocation);
    }

    public boolean loadDrm(File drmFile) {
        if (drmFile == null) {
            return false;
        }
        ProjectSpecification projectSpec = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(drmFile));
            projectSpec = (ProjectSpecification) ois.readObject();

            LOGGER.info("Reading project specification at [" + drmFile + "]");

            if (projectSpec == null) {
                LOGGER.log(Level.WARNING, "Failed to load project specification at [" + drmFile + "]");
                JOptionPane.showMessageDialog(null, "Specification failed load");
            } else {
                // Add root missions
                for (Object m : projectSpec.getRootMissionPlans()) {
                    missionListModel.addElement(m);
                }
//                for (UiFrame uiFrame : uiFrames) {
//                    uiFrame.setGUISpec(projectSpec.getGuiElements());
//                }
                // @todo Clear variables?

                // Load project spec's global variables
                for (String variable : projectSpec.getGlobalVariableToValue().keySet()) {
                    Engine.getInstance().setVariableValue(variable, projectSpec.getGlobalVariableValue(variable));
                }

                Preferences p = Preferences.userRoot();
                try {
                    p.put(LAST_DRM_FILE, drmFile.getAbsolutePath());
                    p.put(LAST_DRM_FOLDER, drmFile.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }
                setTitle("Mission Monitor [" + drmFile.toString() + "]");
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.severe("Class not found exception in DRM load");
            return false;
        } catch (FileNotFoundException ex) {
            LOGGER.severe("DRM File not found");
            return false;
        } catch (InvalidClassException ex) {
            LOGGER.severe("Version mismatch in DRM file");
            return false;
        } catch (IOException ex) {
            LOGGER.severe("IO Exception on DRM load");
            return false;
        } catch (NullPointerException ex) {
            LOGGER.severe("Could not load DRM file");
            return false;
        }
        return true;
    }//GEN-LAST:event_loadDrmBActionPerformed

    private void runBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runBActionPerformed
        MissionPlanSpecification mSpec = (MissionPlanSpecification) planL.getSelectedValue();
        if (mSpec != null) {
            PlanManager pm = Engine.getInstance().spawnRootMission(mSpec);
        }
    }//GEN-LAST:event_runBActionPerformed

    private void loadEpfBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadEpfBActionPerformed
        File environmentLocation = null;

        if (environmentLocation == null) {
            Preferences p = Preferences.userRoot();
            String lastEpfFolder = p.get(LAST_EPF_FOLDER, "");
            JFileChooser chooser = new JFileChooser(lastEpfFolder);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("EPF specification files", "epf");
            chooser.setFileFilter(filter);
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION) {
                environmentLocation = chooser.getSelectedFile();
            }
        }
        loadEpf(environmentLocation);
    }//GEN-LAST:event_loadEpfBActionPerformed

    public void loadEpf(File epfFile) {
        if (epfFile == null) {
            return;
        }
        EnvironmentProperties environmentProperties = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(epfFile));
            environmentProperties = (EnvironmentProperties) ois.readObject();

            LOGGER.info("Reading environment properties at [" + epfFile + "]");

            if (environmentProperties == null) {
                LOGGER.log(Level.WARNING, "Failed to load environment properties at [" + epfFile + "]");
                JOptionPane.showMessageDialog(null, "Environment properties failed load");
            } else {
                Preferences p = Preferences.userRoot();
                try {
                    p.put(LAST_EPF_FILE, epfFile.getAbsolutePath());
                    p.put(LAST_EPF_FOLDER, epfFile.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }

                Engine.getInstance().setEnvironmentProperties(environmentProperties);
            }

        } catch (ClassNotFoundException ex) {
            LOGGER.severe("Class not found exception in EPF load");
        } catch (FileNotFoundException ex) {
            LOGGER.severe("EPF File not found");
        } catch (IOException ex) {
            LOGGER.severe("IO Exception on EPF load");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MissionMonitor().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton loadDrmB;
    private javax.swing.JButton loadEpfB;
    private javax.swing.JPanel missionViewersP;
    private javax.swing.JScrollPane missionsScrollP;
    private javax.swing.JPanel placeholderP;
    private javax.swing.JList planL;
    private javax.swing.JScrollPane planScrollP;
    private javax.swing.JButton runB;
    // End of variables declaration//GEN-END:variables

    @Override
    public void planCreated(PlanManager planManager, MissionPlanSpecification mSpec) {
        // A plan was just created - add a visualization of the Petri Net
        MissionDisplay missionDisplay = new MissionDisplay(this, mSpec, planManager);
        pmToDisplay.put(planManager, missionDisplay);
        missionDisplayList.add(missionDisplay);
        refreshMissionDisplay();
        this.repaint();
    }

    @Override
    public void planStarted(PlanManager planManager) {
    }

    @Override
    public void planEnteredPlace(PlanManager planManager, Place p) {
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place p) {
    }

    @Override
    public void planRepaint(PlanManager planManager) {
    }

    @Override
    public void planFinished(PlanManager planManager) {
        // A plan was just finished - remove the visualization
        MissionDisplay missionDisplay = pmToDisplay.get(planManager);
        if (missionDisplay == null) {
            LOGGER.log(Level.SEVERE, "Could not find MissionDisplay for PlanManager " + planManager);
            return;
        }
        missionDisplayList.remove(missionDisplay);
        refreshMissionDisplay();
        this.repaint();
    }

    @Override
    public void planAborted(PlanManager planManager) {
        // A plan was just aborted - remove the visualization
        MissionDisplay missionDisplay = pmToDisplay.get(planManager);
        if (missionDisplay == null) {
            LOGGER.log(Level.SEVERE, "Could not find MissionDisplay for PlanManager " + planManager);
            return;
        }
        missionDisplayList.remove(missionDisplay);
        refreshMissionDisplay();
        this.repaint();
    }

    public void refreshMissionDisplay() {
        missionViewersP.removeAll();
        missionViewersP.setLayout(new GridBagLayout());
        int rowCount = 0;
        GridBagConstraints c = new GridBagConstraints();
        for (MissionDisplay missionDisplay : missionDisplayList) {
            c.gridx = 0;
            c.gridy = rowCount;
            c.weightx = 1;
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;

            missionViewersP.add(missionDisplay, c);
            rowCount++;
        }
        missionViewersP.revalidate();
    }
}
