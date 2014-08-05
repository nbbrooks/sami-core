package test;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AccessControlException;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.mission.MissionPlanSpecification;
import sami.mission.ProjectSpecification;
import static sami.ui.MissionMonitor.LAST_DRM_FILE;
import static sami.ui.MissionMonitor.LAST_DRM_FOLDER;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class Test {

    static File specLocation = null;
//        File specLocation = new File("C:\\Users\\nbb\\Documents\\pickup.drm");

    public static void main(String[] args) {
                Preferences p = Preferences.userRoot();

            String lastDrmPath = p.get(LAST_DRM_FILE, null);
            if (lastDrmPath != null) {
                specLocation= new File(lastDrmPath);
            }
        if (specLocation == null) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("DREAAM specification files", "drm");
            chooser.setFileFilter(filter);
            int ret = chooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION) {
                specLocation = chooser.getSelectedFile();
            }
        }

        ProjectSpecification projectSpec = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(specLocation));
            projectSpec = (ProjectSpecification) ois.readObject();


            if (projectSpec == null) {
                JOptionPane.showMessageDialog(null, "Specification failed load");
            } else {
                         MissionPlanSpecification cmp1,cmp2;
                for (Object m : projectSpec.getAllMissionPlans()) {
                    MissionPlanSpecification mi = (MissionPlanSpecification)m;
                    System.out.println("test: "+mi.toString());
                    
           
                    
                    if(mi.toString().equals("Return To Base")){
                        cmp1=mi;
                    }
                    if(mi.toString().equals("Anonymous2")){
                        cmp2=mi;
                        System.out.println(mi.getVertexToEventSpecListMap().toString());
                    }
                    
//                    missionListModel.addElement(m);
                }
                
                projectSpec.printDetails();
                
                try {
                    p.put(LAST_DRM_FILE, specLocation.getAbsolutePath());
                    p.put(LAST_DRM_FOLDER, specLocation.getParent());
                } catch (AccessControlException e) {
                }
//                System.out.println("T: "+);
                    
//                for (UiFrame uiFrame : uiFrames) {
//                    uiFrame.setGUISpec(projectSpec.getGuiElements());
//                }
//s
            }

        } catch (ClassNotFoundException ex) {
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
    }

//    loadDrm(specLocation);
}
