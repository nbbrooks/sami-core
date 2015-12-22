package sami.logging;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import sami.CoreHelper;
import sami.engine.PlanManager;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.ui.MissionMonitor;

/**
 *
 * @author nbb
 */
public class Recorder {

    private static final Logger LOGGER = Logger.getLogger(Recorder.class.getName());
    /*
    IMPORTANT!!!
    WHEN CHANGING ENABLED, FORCE A CLEAN AND REBUILD ON EVERY FILE THAT REFERENCES THIS BECAUSE NETBEANS DOESN'T!
    A FORCED CLEAN AND REBUILD ON PROJECT DOENS'T SEEM TO BE SUFFICIENT?
    //@todo verify
    */
    public static final boolean ENABLED = false;

    FileWriter playbackWriter;

    private static class RecorderHolder {

        public static final Recorder INSTANCE = new Recorder();
    }

    public static Recorder getInstance() {
        return RecorderHolder.INSTANCE;
    }

    private Recorder() {
        if (!ENABLED) {
            return;
        }
        try {
            // Create playback file
            playbackWriter = new FileWriter(new File(MissionMonitor.LOG_DIRECTORY + "recorder.log"));
            playbackWriter.write(System.currentTimeMillis() + "\t" + "START_TIME" + "\n");
            playbackWriter.write(System.currentTimeMillis() + "\t" + "RANDOM_SEED" + "\t" + CoreHelper.START_TIME + "\n");
            playbackWriter.flush();
        } catch (IOException ex) {
            Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void spawnRootMission(MissionPlanSpecification mSpec, PlanManager pm) {
        if (!ENABLED) {
            return;
        }
        try {
            playbackWriter.write(System.currentTimeMillis() + "\t" + "SPAWN_ROOT_MISSION" + "\t" + mSpec.getName() + "\t" + pm.missionId + "\t" + pm.getPlanName() + "\n");
            playbackWriter.flush();
        } catch (IOException ex) {
            Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void eventGenerated(InputEvent generatedEvent) {
//        if (!ENABLED) {
//            return;
//        }
//        try {
//            String eventString = generatedEvent.getClass().getCanonicalName();
//            Class recursionClass = generatedEvent.getClass();
//            for (Field field : recursionClass.getDeclaredFields()) {
//                field.setAccessible(true);
//                eventString += "\t" + field.getName() + "\t" + field.get(generatedEvent);
//            }
//            // Recurse up to Object.class
//            while (recursionClass.getSuperclass() != null) {
//                recursionClass = recursionClass.getSuperclass();
//                for (Field field : recursionClass.getDeclaredFields()) {
//                    field.setAccessible(true);
//                    eventString += "\t" + field.getName() + "\t" + field.get(generatedEvent);
//                }
//            }
//
//            try {
//                playbackWriter.write(System.currentTimeMillis() + "\t" + "EVENT_GENERATED" + "\t" + eventString + "\n");
//                playbackWriter.flush();
//            } catch (IOException ex) {
//                Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public void writeInstantiatedIds(PlanManager pm) {
        if (!ENABLED) {
            return;
        }
        // Special case for OperatorInterruptReceived
        for (Vertex v : pm.getMSpec().getTransientGraph().getVertices()) {
            if (v instanceof Place) {
                Place place = (Place) v;
                String idString = place.getVertexId() + "";
                for (OutputEvent outputEvent : place.getOutputEvents()) {
                    idString += "\t" + outputEvent.getClass().getCanonicalName() + "\t" + outputEvent.getId();
                }
                try {
                    playbackWriter.write(System.currentTimeMillis() + "\t" + "INSTANTIATED_PLACE" + "\t" + pm.missionId + "\t" + "[" + pm.getPlanName() + "]" + "\t" + idString + "\n");
                    playbackWriter.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (v instanceof Transition) {
                Transition transition = (Transition) v;
                String idString = transition.getVertexId() + "";
                for (InputEvent inputEvent : transition.getInputEvents()) {
                    idString += "\t" + inputEvent.getClass().getCanonicalName() + "\t" + inputEvent.getId();
                }
                try {
                    playbackWriter.write(System.currentTimeMillis() + "\t" + "INSTANTIATED_TRANSITION" + "\t" + pm.missionId + "\t" + "[" + pm.getPlanName() + "]" + "\t" + idString + "\n");
                    playbackWriter.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public void recordPlanState(PlanManager pm) {
        if (!ENABLED) {
            return;
        }
        String printString = "";
        for (Vertex v : pm.getMSpec().getTransientGraph().getVertices()) {
            printString += v.getVertexId() + "\t" + v.getVisibilityMode() + "\t" + "[" + v.getShortTag() + "]" + "\t";
        }
        try {
            playbackWriter.write(System.currentTimeMillis() + "\t" + "PM_STATE" + pm.missionId + "\t" + pm.getPlanName() + "\t" + "\t" + printString + "\n");
            playbackWriter.flush();
        } catch (IOException ex) {
            Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void recordScreenshot() {
        if (!ENABLED) {
            return;
        }
        Date date = new Date();
        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(date);
        try {
            BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            File file = new File(MissionMonitor.LOG_DIRECTORY + "/" + timestamp + ".png");
            ImageIO.write(image, "png", file);
            playbackWriter.write(date.getTime() + "\t" + "SCREENSHOT" + "\t" + file.getAbsolutePath() + "\n");
            playbackWriter.flush();
        } catch (IOException ex) {
            Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AWTException ex) {
            Logger.getLogger(Recorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
