package sami.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import sami.CoreHelper;
import sami.LoggerFormatter;
import sami.config.DomainConfigManager;
import sami.engine.Engine;
import sami.engine.Mediator;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.ProjectListenerInt;
import sami.mission.ProjectSpecification;
import sami.mission.Transition;
import sami.service.ServiceServer;
import sami.uilanguage.LocalUiClientServer;
import sami.uilanguage.UiFrame;

/**
 *
 * @author pscerri
 */
public class MissionMonitor extends javax.swing.JFrame implements PlanManagerListenerInt, ProjectListenerInt {

    private static final Logger LOGGER = Logger.getLogger(MissionMonitor.class.getName());
    public static final String LOG_DIRECTORY = "run/logs/" + CoreHelper.LOGGING_TIMESTAMP + "/";
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
        loadFiles();
        LOGGER.info("drm: " + Mediator.getInstance().getProjectFile().getAbsolutePath());
        LOGGER.info("epf: " + Mediator.getInstance().getEnvironmentFile().getAbsolutePath());
        LOGGER.info("dcf: " + DomainConfigManager.getInstance().getDomainConfigurationFile().getAbsolutePath());
        CoreHelper.copyLoadedDrmToDirectory(LOG_DIRECTORY);
        CoreHelper.copyLoadedEpfToDirectory(LOG_DIRECTORY);
        CoreHelper.copyLoadedDcfToDirectory(LOG_DIRECTORY);

        initComponents();

        (new FrameManager()).setVisible(true);

//        InformationGenerationFrame igf = new InformationGenerationFrame();
//        igf.setVisible(true);
        LocalUiClientServer clientServer = new LocalUiClientServer();
        Engine.getInstance().setUiClient(clientServer);
        Engine.getInstance().setUiServer(clientServer);
        // Set Engine singleton's services server
        Engine.getInstance().setServiceServer(new ServiceServer());

        for (String className : DomainConfigManager.getInstance().getDomainConfiguration().uiList) {
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
            }
        }

//        new StateManagerAccessor();
        missionViewersP.setLayout(new BoxLayout(missionViewersP, BoxLayout.Y_AXIS));
        planL.setModel(missionListModel);

//        (new AgentPlatform()).showMonitor();
        FrameManager.restoreLayout();

        Mediator.getInstance().addProjectListener(this);
        Engine.getInstance().addListener(this);

        projectUpdated();
    }

    private void loadFiles() {
        boolean success;
        int answer;

        // Try to load the last used DRM file
        success = Mediator.getInstance().openLatestProject();
        if (!success) {
            LOGGER.severe("Failed to load last used DRM");
            answer = JOptionPane.showOptionDialog(null, "Failed to load last used DRM: Load different DRM or exit?", "Load different DRM?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                success = Mediator.getInstance().openProjectFromBrowser();
            } else {
                System.exit(0);
            }
        }
        while (!success) {
            LOGGER.severe("Failed to load specified DRM");
            answer = JOptionPane.showOptionDialog(null, "Failed to load specified DRM: Load different DRM or exit?", "Load different DRM?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                success = Mediator.getInstance().openProjectFromBrowser();
            } else {
                System.exit(0);
            }
        }

        // Try to load the last used EPF file
        success = Mediator.getInstance().openLatestEnvironment();
        if (!success) {
            LOGGER.severe("Failed to load last used EPF");
            answer = JOptionPane.showOptionDialog(null, "Failed to load last used EPF: Load different EPF or ignore?", "Load different EPF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                success = Mediator.getInstance().openEnvironmentFromBrowser();
            } else {
                Mediator.getInstance().newEnvironment();
                success = true;
            }
        }
        while (!success) {
            LOGGER.severe("Failed to load specified EPF");
            answer = JOptionPane.showOptionDialog(null, "Failed to load specified EPF: Load different EPF or ignore?", "Load different EPF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                success = Mediator.getInstance().openEnvironmentFromBrowser();
            } else {
                Mediator.getInstance().newEnvironment();
                success = true;
            }
        }

        // Try to load the last used DCF file
        success = DomainConfigManager.getInstance().openLatestDomainConfiguration();
        if (!success) {
            LOGGER.severe("Failed to load last used DCF");
            answer = JOptionPane.showOptionDialog(null, "Failed to load last used DCF: Load different DCF or exit?", "Load different DCF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                success = DomainConfigManager.getInstance().openDomainConfigurationFromBrowser();
            } else {
                System.exit(0);
            }
        }
        while (!success) {
            LOGGER.severe("Failed to load specified DCF");
            answer = JOptionPane.showOptionDialog(null, "Failed to load specified DCF: Load different DCF or exit?", "Load different DCF?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (answer == JOptionPane.YES_OPTION) {
                success = DomainConfigManager.getInstance().openDomainConfigurationFromBrowser();
            } else {
                System.exit(0);
            }
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

    @Override
    public void projectUpdated() {
        applyProjectValues();
    }

    private void applyProjectValues() {
        ProjectSpecification spec = Mediator.getInstance().getProject();
        // Clear root missions 
        missionListModel.removeAllElements();
            // @todo Clear global variables?

        // Add root missions and global variables
        for (Object m : spec.getRootMissionPlans()) {
            missionListModel.addElement(m);
        }
        Engine.getInstance().clearGlobalVariables();
        for (String variable : spec.getGlobalVariableToValue().keySet()) {
            // @todo should Engine implement ProjectListenerInt and do this itself?
            Engine.getInstance().setVariableValue(variable, spec.getGlobalVariableValue(variable), null);
        }
        drmName.setText(Mediator.getInstance().getProjectFile().toString());
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
        drmName = new javax.swing.JLabel();

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

        drmName.setText(" ");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(drmName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(planScrollP, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                            .add(runB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(loadDrmB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(loadEpfB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(missionsScrollP)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(drmName)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(missionsScrollP, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE)
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
        boolean success = Mediator.getInstance().openProjectFromBrowser();
        CoreHelper.copyLoadedDrmToDirectory(LOG_DIRECTORY);
        if (!success) {
            JOptionPane.showMessageDialog(null, "Failed to load project");
        }
    }//GEN-LAST:event_loadDrmBActionPerformed

    private void runBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runBActionPerformed
        MissionPlanSpecification mSpec = (MissionPlanSpecification) planL.getSelectedValue();
        if (mSpec != null) {
            PlanManager pm = Engine.getInstance().spawnRootMission(mSpec);
        }
    }//GEN-LAST:event_runBActionPerformed

    private void loadEpfBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadEpfBActionPerformed
        boolean success = Mediator.getInstance().openEnvironmentFromBrowser();
        CoreHelper.copyLoadedEpfToDirectory(LOG_DIRECTORY);
        if (!success) {
            JOptionPane.showMessageDialog(null, "Failed to load environment");
        }
    }//GEN-LAST:event_loadEpfBActionPerformed

    public static void setUpLogging() {
        LOGGER.info("Log directory is " + LOG_DIRECTORY);
        try {
            // Create directory
            new File(LOG_DIRECTORY).mkdir();
            // Add log file
            FileHandler fh = new FileHandler(LOG_DIRECTORY + "sami.log", 50000, 1);
            fh.setFormatter(new LoggerFormatter());
            fh.setLevel(Level.INFO);
            LOGGER.addHandler(fh);
        } catch (IOException ex) {
            Logger.getLogger(MissionMonitor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MissionMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        MissionMonitor.setUpLogging();

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MissionMonitor().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel drmName;
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
    public void planInstantiated(PlanManager planManager) {
    }

    @Override
    public void planEnteredPlace(PlanManager planManager, Place p) {
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place p) {
    }

    @Override
    public void planExecutedTransition(PlanManager planManager, Transition transition) {
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
