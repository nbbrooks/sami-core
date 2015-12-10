package sami.config;

import java.awt.Color;
import java.io.File;
import java.security.AccessControlException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author nbb
 */
public class DomainConfigF extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(DomainConfigF.class.getName());
    private DomainConfig domainConfiguration;
    private File configLocation = null;

    /**
     * Creates new form ConfigurationF
     */
    public DomainConfigF() {
        initComponents();
        boolean success = DomainConfigManager.getInstance().openLatestDomainConfiguration();
        if (!success) {
            JOptionPane.showMessageDialog(null, "Failed to load previous domain configuration, opening new configuration");
            DomainConfigManager.getInstance().newDomainConfiguration();
        }
        domainConfiguration = DomainConfigManager.getInstance().getDomainConfiguration();
        configLocation = DomainConfigManager.getInstance().getDomainConfigurationFile();
        refreshComponents();
    }

    /**
     * Write DCF's CFG locations to corresponding text fields and update
     * completeness coloring
     */
    private void refreshComponents() {
        if (configLocation != null) {
            setTitle(configLocation.getAbsolutePath());
        } else {
            setTitle("Not saved");
        }

        nameTF.setText(domainConfiguration.domainName);
        descriptionTA.setText(domainConfiguration.domainDescription);
        agentTF.setText(domainConfiguration.agentTreeFilePath);
        assetTF.setText(domainConfiguration.assetTreeFilePath);
        componentTF.setText(domainConfiguration.componentGeneratorListFilePath);
        messageTF.setText(domainConfiguration.fromUiMessageGeneratorListFilePath);
        eventTF.setText(domainConfiguration.eventTreeFilePath);
        handlerTF.setText(domainConfiguration.eventHandlerMappingFilePath);
        markupTF.setText(domainConfiguration.markupTreeFilePath);
        serverTF.setText(domainConfiguration.serverListFilePath);
        taskTF.setText(domainConfiguration.taskTreeFilePath);
        uiTF.setText(domainConfiguration.uiListFilePath);
        refreshCompleteComponent();
    }

    public void refreshCompleteComponent() {
        // Check that we have a DCF with fully defined values
        if (domainConfiguration == null || !domainConfiguration.complete) {
            completeB.setBackground(Color.RED);
            return;
        }

        // Check that we have those files locally
        //  Don't HAVE to have the files locally as the DCF stores the values when it is saved,
        //  but since we are editing the DCF in this frame it makes sense to show if the files 
        //  were found in case you accidentally provided the wrong path
        String[] filesToCheck = new String[]{
            domainConfiguration.agentTreeFilePath,
            domainConfiguration.assetTreeFilePath,
            domainConfiguration.componentGeneratorListFilePath,
            domainConfiguration.fromUiMessageGeneratorListFilePath,
            domainConfiguration.eventHandlerMappingFilePath,
            domainConfiguration.eventTreeFilePath,
            domainConfiguration.markupTreeFilePath,
            domainConfiguration.serverListFilePath,
            domainConfiguration.taskTreeFilePath,
            domainConfiguration.uiListFilePath};
        File file;
        for (int i = 0; i < filesToCheck.length; i++) {
            file = new File(filesToCheck[i]);
            if (!file.exists()) {
                completeB.setBackground(Color.RED);
                return;
            }
        }
        completeB.setBackground(Color.GREEN);
    }

    private File selectConf(String text) {
        JFileChooser chooser = new JFileChooser();
        // Try to set the chooser's directory to the last directory a DRM file was successfully loaded from
        Preferences p = null;
        try {
            p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(DomainConfigManager.LAST_CFG_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.warning("Last CFG folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.warning("Last CFG folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        // Limit the chooser to .cfg files
        FileNameExtensionFilter filter = new FileNameExtensionFilter(text + " (.cfg)", "cfg");
        chooser.setFileFilter(filter);

        int ret = chooser.showOpenDialog(null);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        if (p != null) {
            try {
                p.put(DomainConfigManager.LAST_CFG_FOLDER, chooser.getSelectedFile().getParent());
            } catch (AccessControlException e) {
                LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
            }
        }
        return chooser.getSelectedFile();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        agentL = new javax.swing.JLabel();
        agentB = new javax.swing.JButton();
        nameTF = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionTA = new javax.swing.JTextArea();
        nameL = new javax.swing.JLabel();
        descriptionL = new javax.swing.JLabel();
        assetB = new javax.swing.JButton();
        assetL = new javax.swing.JLabel();
        componentB = new javax.swing.JButton();
        componentL = new javax.swing.JLabel();
        eventB = new javax.swing.JButton();
        eventL = new javax.swing.JLabel();
        handlerB = new javax.swing.JButton();
        handlerL = new javax.swing.JLabel();
        markupB = new javax.swing.JButton();
        markupL = new javax.swing.JLabel();
        serverB = new javax.swing.JButton();
        serverL = new javax.swing.JLabel();
        taskB = new javax.swing.JButton();
        taskL = new javax.swing.JLabel();
        uiL = new javax.swing.JLabel();
        uiTF = new javax.swing.JTextField();
        uiB = new javax.swing.JButton();
        taskTF = new javax.swing.JTextField();
        agentTF = new javax.swing.JTextField();
        assetTF = new javax.swing.JTextField();
        componentTF = new javax.swing.JTextField();
        eventTF = new javax.swing.JTextField();
        handlerTF = new javax.swing.JTextField();
        markupTF = new javax.swing.JTextField();
        serverTF = new javax.swing.JTextField();
        saveB = new javax.swing.JButton();
        openB = new javax.swing.JButton();
        saveAsB = new javax.swing.JButton();
        completeB = new javax.swing.JButton();
        messageB = new javax.swing.JButton();
        messageTF = new javax.swing.JTextField();
        messageL = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        agentL.setText("Agent tree file:");

        agentB.setText("Select");
        agentB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                agentBActionPerformed(evt);
            }
        });

        descriptionTA.setColumns(20);
        descriptionTA.setRows(5);
        jScrollPane1.setViewportView(descriptionTA);

        nameL.setText("Domain name:");

        descriptionL.setText("Domain description:");

        assetB.setText("Select");
        assetB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                assetBActionPerformed(evt);
            }
        });

        assetL.setText("Asset tree file:");

        componentB.setText("Select");
        componentB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                componentBActionPerformed(evt);
            }
        });

        componentL.setText("Component generator list file:");

        eventB.setText("Select");
        eventB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                eventBActionPerformed(evt);
            }
        });

        eventL.setText("Event tree file:");

        handlerB.setText("Select");
        handlerB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handlerBActionPerformed(evt);
            }
        });

        handlerL.setText("Event handler mapping file:");

        markupB.setText("Select");
        markupB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                markupBActionPerformed(evt);
            }
        });

        markupL.setText("Markup tree file:");

        serverB.setText("Select");
        serverB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverBActionPerformed(evt);
            }
        });

        serverL.setText("Server list file:");

        taskB.setText("Select");
        taskB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                taskBActionPerformed(evt);
            }
        });

        taskL.setText("Task tree file:");

        uiL.setText("UI list file:");

        uiB.setText("Select");
        uiB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uiBActionPerformed(evt);
            }
        });

        saveB.setText("Save");
        saveB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBActionPerformed(evt);
            }
        });

        openB.setText("Open");
        openB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openBActionPerformed(evt);
            }
        });

        saveAsB.setText("Save As");
        saveAsB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsBActionPerformed(evt);
            }
        });

        messageB.setText("Select");
        messageB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                messageBActionPerformed(evt);
            }
        });

        messageL.setText("From UI message generator list file:");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(layout.createSequentialGroup()
                        .add(nameL)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(nameTF))
                    .add(jScrollPane1)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(componentTF, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 678, Short.MAX_VALUE)
                            .add(messageTF))
                        .add(6, 6, 6)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(componentB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 93, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(messageB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 93, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(agentTF, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 678, Short.MAX_VALUE)
                            .add(assetTF))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(assetB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 93, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, agentB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 93, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(layout.createSequentialGroup()
                                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, handlerTF)
                                        .add(eventTF))
                                    .add(6, 6, 6))
                                .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, serverTF)
                                        .add(markupTF))
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                                .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, uiTF)
                                        .add(taskTF))
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(descriptionL)
                                    .add(agentL)
                                    .add(assetL)
                                    .add(messageL)
                                    .add(componentL)
                                    .add(layout.createSequentialGroup()
                                        .add(openB)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(saveB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 81, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(saveAsB))
                                    .add(uiL)
                                    .add(eventL)
                                    .add(handlerL)
                                    .add(markupL)
                                    .add(serverL)
                                    .add(taskL))
                                .add(422, 422, 422)))
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(completeB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(taskB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                            .add(serverB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(markupB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(uiB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(handlerB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(eventB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .add(12, 12, 12))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(nameL)
                    .add(nameTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(descriptionL)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 48, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(agentL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(agentTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(agentB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 48, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(assetL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(assetTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(componentL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(componentTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(layout.createSequentialGroup()
                                .add(assetB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(componentB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(messageL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(messageTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(messageB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 52, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(eventL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(eventTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(eventB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(handlerL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(handlerTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(handlerB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(markupL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(markupTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(markupB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(serverL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(serverTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(serverB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(taskL)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(taskTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(taskB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 51, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(uiL)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(uiTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(uiB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(completeB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(saveAsB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(openB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(saveB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void agentBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_agentBActionPerformed
        File file = selectConf(agentL.getText());
        if (file != null) {
            agentTF.setText(file.getAbsolutePath());
            domainConfiguration.agentTreeFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_agentBActionPerformed

    private void assetBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_assetBActionPerformed
        File file = selectConf(assetL.getText());
        if (file != null) {
            assetTF.setText(file.getAbsolutePath());
            domainConfiguration.assetTreeFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
    }//GEN-LAST:event_assetBActionPerformed

    private void componentBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_componentBActionPerformed
        File file = selectConf(componentL.getText());
        if (file != null) {
            componentTF.setText(file.getAbsolutePath());
            domainConfiguration.componentGeneratorListFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_componentBActionPerformed

    private void eventBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_eventBActionPerformed
        File file = selectConf(eventL.getText());
        if (file != null) {
            eventTF.setText(file.getAbsolutePath());
            domainConfiguration.eventTreeFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_eventBActionPerformed

    private void handlerBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_handlerBActionPerformed
        File file = selectConf(handlerL.getText());
        if (file != null) {
            handlerTF.setText(file.getAbsolutePath());
            domainConfiguration.eventHandlerMappingFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_handlerBActionPerformed

    private void markupBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_markupBActionPerformed
        File file = selectConf(markupL.getText());
        if (file != null) {
            markupTF.setText(file.getAbsolutePath());
            domainConfiguration.markupTreeFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_markupBActionPerformed

    private void serverBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverBActionPerformed
        File file = selectConf(serverL.getText());
        if (file != null) {
            serverTF.setText(file.getAbsolutePath());
            domainConfiguration.serverListFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_serverBActionPerformed

    private void taskBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_taskBActionPerformed
        File file = selectConf(taskL.getText());
        if (file != null) {
            taskTF.setText(file.getAbsolutePath());
            domainConfiguration.taskTreeFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_taskBActionPerformed

    private void uiBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uiBActionPerformed
        File file = selectConf(uiL.getText());
        if (file != null) {
            uiTF.setText(file.getAbsolutePath());
            domainConfiguration.uiListFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_uiBActionPerformed

    private void saveBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBActionPerformed
        domainConfiguration.domainName = nameTF.getText();
        domainConfiguration.domainDescription = descriptionTA.getText();
        DomainConfigManager.getInstance().saveDomainConfiguration();
    }//GEN-LAST:event_saveBActionPerformed

    private void openBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openBActionPerformed
        boolean success = DomainConfigManager.getInstance().openDomainConfigurationFromBrowser();
        if (!success) {
            JOptionPane.showMessageDialog(null, "Failed to load domain configuration, opening new configuration");
            DomainConfigManager.getInstance().newDomainConfiguration();
        }
        domainConfiguration = DomainConfigManager.getInstance().getDomainConfiguration();
        configLocation = DomainConfigManager.getInstance().getDomainConfigurationFile();
        refreshComponents();
    }//GEN-LAST:event_openBActionPerformed

    private void saveAsBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsBActionPerformed
        domainConfiguration.domainName = nameTF.getText();
        domainConfiguration.domainDescription = descriptionTA.getText();
        DomainConfigManager.getInstance().saveDomainConfigurationAs();
    }//GEN-LAST:event_saveAsBActionPerformed

    private void messageBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_messageBActionPerformed
        File file = selectConf(messageL.getText());
        if (file != null) {
            messageTF.setText(file.getAbsolutePath());
            domainConfiguration.fromUiMessageGeneratorListFilePath = file.getAbsolutePath();
            domainConfiguration.loadCfgValues();
        }
        refreshCompleteComponent();
    }//GEN-LAST:event_messageBActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DomainConfigF.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DomainConfigF.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DomainConfigF.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DomainConfigF.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DomainConfigF().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton agentB;
    private javax.swing.JLabel agentL;
    private javax.swing.JTextField agentTF;
    private javax.swing.JButton assetB;
    private javax.swing.JLabel assetL;
    private javax.swing.JTextField assetTF;
    private javax.swing.JButton completeB;
    private javax.swing.JButton componentB;
    private javax.swing.JLabel componentL;
    private javax.swing.JTextField componentTF;
    private javax.swing.JLabel descriptionL;
    private javax.swing.JTextArea descriptionTA;
    private javax.swing.JButton eventB;
    private javax.swing.JLabel eventL;
    private javax.swing.JTextField eventTF;
    private javax.swing.JButton handlerB;
    private javax.swing.JLabel handlerL;
    private javax.swing.JTextField handlerTF;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton markupB;
    private javax.swing.JLabel markupL;
    private javax.swing.JTextField markupTF;
    private javax.swing.JButton messageB;
    private javax.swing.JLabel messageL;
    private javax.swing.JTextField messageTF;
    private javax.swing.JLabel nameL;
    private javax.swing.JTextField nameTF;
    private javax.swing.JButton openB;
    private javax.swing.JButton saveAsB;
    private javax.swing.JButton saveB;
    private javax.swing.JButton serverB;
    private javax.swing.JLabel serverL;
    private javax.swing.JTextField serverTF;
    private javax.swing.JButton taskB;
    private javax.swing.JLabel taskL;
    private javax.swing.JTextField taskTF;
    private javax.swing.JButton uiB;
    private javax.swing.JLabel uiL;
    private javax.swing.JTextField uiTF;
    // End of variables declaration//GEN-END:variables
}
