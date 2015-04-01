package sami;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.config.DomainConfigManager;
import sami.engine.Mediator;

/**
 *
 * @author nbb
 */
public class CoreHelper {

    private static final Logger LOGGER = Logger.getLogger(CoreHelper.class.getName());
    public static final long START_TIME = System.currentTimeMillis();
    public static final Date LOGGING_DATE = new Date();
    public static final String LOGGING_TIMESTAMP = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(LOGGING_DATE);
    public static final Random RANDOM = new Random(START_TIME);
    private static long id = 0;

    public static long getNewId() {
        return id++;
    }
    
    public static void copyLoadedDcfToFile(File destination) {
        File loadedDcf = DomainConfigManager.getInstance().getDomainConfigurationFile();
        if (loadedDcf != null) {
            try {
                Files.copy(loadedDcf.toPath(), destination.toPath());
                LOGGER.info("Copied DCF \"" + loadedDcf.getAbsolutePath() + "\" to \"" + destination.getAbsolutePath() + "\"");
            } catch (IOException ex) {
                LOGGER.severe("Failed to copy DCF \"" + loadedDcf.getAbsolutePath() + "\" to \"" + destination.getAbsolutePath() + "\"");
                ex.printStackTrace();
            }
        } else {
            LOGGER.severe("No loaded DCF to copy");
        }
    }

    public static void copyLoadedDcfToDirectory(String destinationDirectory) {
        File directory = new File(destinationDirectory);
        if (!directory.isDirectory()) {
            LOGGER.severe("Failed to copy DCF, \"" + destinationDirectory + "\" is not a valid directory path");
            return;
        }
        File loadedDcf = DomainConfigManager.getInstance().getDomainConfigurationFile();
        File copy = new File(directory.getAbsolutePath() + File.separator + loadedDcf.getName());
        copyLoadedDcfToFile(copy);
    }

    public static void copyLoadedDrmToFile(File destination) {
        File loadedDrm = Mediator.getInstance().getProjectFile();
        if (loadedDrm != null) {
            try {
                Files.copy(loadedDrm.toPath(), destination.toPath());
                LOGGER.info("Copied DRM \"" + loadedDrm.getAbsolutePath() + "\" to \"" + destination.getAbsolutePath() + "\"");
            } catch (IOException ex) {
                LOGGER.severe("Failed to copy DRM \"" + loadedDrm.getAbsolutePath() + "\" to \"" + destination.getAbsolutePath() + "\"");
                ex.printStackTrace();
            }
        } else {
            LOGGER.severe("No loaded DRM to copy");
        }
    }

    public static void copyLoadedDrmToDirectory(String destinationDirectory) {
        File directory = new File(destinationDirectory);
        if (!directory.isDirectory()) {
            LOGGER.severe("Failed to copy DRM, \"" + destinationDirectory + "\" is not a valid directory path");
            return;
        }
        File loadedDrm = Mediator.getInstance().getProjectFile();
        File copy = new File(directory.getAbsolutePath() + File.separator + loadedDrm.getName());
        copyLoadedDrmToFile(copy);
    }

    public static void copyLoadedEpfToFile(File destination) {
        File loadedEpf = Mediator.getInstance().getEnvironmentFile();
        if (loadedEpf != null) {
            try {
                Files.copy(loadedEpf.toPath(), destination.toPath());
                LOGGER.info("Copied EPF \"" + loadedEpf.getAbsolutePath() + "\" to \"" + destination.getAbsolutePath() + "\"");
            } catch (IOException ex) {
                LOGGER.severe("Failed to copy EPF \"" + loadedEpf.getAbsolutePath() + "\" to \"" + destination.getAbsolutePath() + "\"");
                Logger.getLogger(CoreHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.severe("No loaded EPF to copy");
        }
    }

    public static void copyLoadedEpfToDirectory(String destinationDirectory) {
        File directory = new File(destinationDirectory);
        if (!directory.isDirectory()) {
            LOGGER.severe("Failed to copy EPF, \"" + destinationDirectory + "\" is not a valid directory path");
            return;
        }
        File loadedEpf = Mediator.getInstance().getEnvironmentFile();
        File copy = new File(directory.getAbsolutePath() + File.separator + loadedEpf.getName());
        copyLoadedEpfToFile(copy);
    }

    public static String shorten(String full, int maxLength) {
        String reduced = "";
        int upperCount = 0;
        for (char c : full.toCharArray()) {
            if (Character.isUpperCase(c) || c == '.') {
                upperCount++;
            }
        }
        int charPerUpper = maxLength / Math.max(1, upperCount); // prevent divide by 0
        int lowerCaseAfterUpperCount = 0;
        for (int i = 0; i < full.length(); i++) {
            if (Character.isUpperCase(full.charAt(i)) || full.charAt(i) == '.') {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount = 0;
            } else if (lowerCaseAfterUpperCount < charPerUpper) {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount++;
            }
        }
        return reduced;
    }

    public static String getUniqueName(String name, ArrayList<String> existingNames) {
        boolean invalidName = existingNames.contains(name);
        while (invalidName) {
            int index = name.length() - 1;
            if ((int) name.charAt(index) < (int) '0' || (int) name.charAt(index) > (int) '9') {
                // name does not end with a number - attach a "2"
                name += "2";
            } else {
                // Find the number the name ends with and increment it
                int numStartIndex = -1, numEndIndex = -1;
                while (index >= 0) {
                    if ((int) name.charAt(index) >= (int) '0' && (int) name.charAt(index) <= (int) '9') {
                        if (numEndIndex == -1) {
                            numEndIndex = index;
                        }
                    } else if (numEndIndex != -1) {
                        numStartIndex = index + 1;
                        break;
                    }
                    index--;
                }
                int number = Integer.parseInt(name.substring(numStartIndex, numEndIndex + 1));
                name = name.substring(0, numStartIndex) + (number + 1);
            }
            invalidName = existingNames.contains(name);
        }
        return name;
    }

}
