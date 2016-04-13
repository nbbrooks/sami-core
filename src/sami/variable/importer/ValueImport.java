package sami.variable.importer;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.Conversion;
import sami.area.Area2D;
import static sami.engine.Mediator.LAST_EPF_FOLDER;
import sami.path.Location;
import sami.path.PathUtm;
import sami.variable.Variable.ClassQuantity;

/**
 *
 * @author nbb
 */
public class ValueImport {

    private static final Logger LOGGER = Logger.getLogger(ValueImport.class.getName());

    public static final String LAST_IMPORT_FILE = "LAST_IMPORT_FILE_CRW";
    public static final String LAST_IMPORT_FOLDER = "LAST_IMPORT_FOLDER_CRW";

    private static class ValueImportHolder {

        public static final ValueImport INSTANCE = new ValueImport();
    }

    public static ValueImport getInstance() {
        return ValueImportHolder.INSTANCE;
    }

    private ValueImport() {
    }

    public Object importValue(Class valueClass, ClassQuantity classQuantity) {
        JFileChooser chooser = new JFileChooser();
        File importFile;
        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                String folderPath = p.get(LAST_EPF_FOLDER, "");
                if (folderPath == null) {
                    LOGGER.severe("Last IMPORT folder preferences entry was NULL");
                } else {
                    File currentFolder = new File(folderPath);
                    if (!currentFolder.isDirectory()) {
                        LOGGER.severe("Last IMPORT folder preferences entry is not a folder: " + currentFolder.getAbsolutePath());
                    } else {
                        chooser.setCurrentDirectory(currentFolder);
                    }
                }
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text values", "txt");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            importFile = chooser.getSelectedFile();
            return importValue(valueClass, classQuantity, importFile);
        }
        LOGGER.info("Canceled value import for " + classQuantity + " of class " + valueClass.getSimpleName());
        return null;
    }

    public Object importValue(Class valueClass, ClassQuantity classQuantity, File importFile) {
        if (valueClass == null) {
            LOGGER.severe("Could not import value, value class was NULL");
            return null;
        }
        if (classQuantity == null) {
            LOGGER.severe("Could not import value, class quantity was NULL");
            return null;
        }
        if (importFile == null) {
            LOGGER.severe("Could not import value, file was NULL");
            return null;
        }

        LOGGER.info("Importing value of class " + valueClass.getSimpleName() + " and quantity " + classQuantity + " from file " + importFile.toString());
        try {
            BufferedReader br = new BufferedReader(new FileReader(importFile));

            //
            // Location.class
            //
            if (valueClass == Location.class) {
                switch (classQuantity) {
                    case SINGLE:
                        try {
                            String line = br.readLine();
                            while (line != null) {
                                if (line.trim().startsWith("#")) {
                                    // Comment line
                                    line = br.readLine();
                                    continue;
                                }
                                int split = line.indexOf(",");
                                if (split == -1) {
                                    LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                    line = br.readLine();
                                    continue;
                                }
                                String lat = line.substring(0, split);
                                String lon = line.substring(split + 1);
                                try {
                                    double pointLat = new Double(lat).doubleValue();
                                    double pointLon = new Double(lon).doubleValue();
                                    LatLon latLon = new LatLon(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon));
                                    Location location2 = Conversion.latLonToLocation(latLon);
                                    if (location2 != null) {
                                        return location2;
                                    }
                                } catch (NumberFormatException ex) {
                                    LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                    line = br.readLine();
                                    continue;
                                }
                                line = br.readLine();
                            }
                        } finally {
                            br.close();
                        }
                        break;
                    case ARRAY_LIST:
                        boolean foundListStartBracket = false,
                         foundPathEndBracket = false;
                        ArrayList<Location> locations = new ArrayList<Location>();
                        try {
                            String line = br.readLine();
                            while (line != null) {
                                if (line.trim().startsWith("#")) {
                                    // Comment line
                                    line = br.readLine();
                                    continue;
                                }
                                if (!foundListStartBracket) {
                                    if (line.startsWith("[")) {
                                        foundListStartBracket = true;
                                    } else {
                                        LOGGER.severe("Expected '[': Failed to process line from value import: \"" + line + "\"");
                                    }
                                } else if (!foundPathEndBracket) {
                                    if (line.startsWith("]")) {
                                        return locations;
                                    } else {
                                        int split = line.indexOf(",");
                                        if (split == -1) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                        String lat = line.substring(0, split);
                                        String lon = line.substring(split + 1);
                                        try {
                                            double pointLat = new Double(lat).doubleValue();
                                            double pointLon = new Double(lon).doubleValue();
                                            LatLon latLon = new LatLon(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon));
                                            Location location2 = Conversion.latLonToLocation(latLon);
                                            if (location2 != null) {
                                                locations.add(location2);
                                            }
                                        } catch (NumberFormatException ex) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                    }
                                }
                                line = br.readLine();
                            }
                        } finally {
                            br.close();
                        }
                        break;
                    default:
                        LOGGER.severe("Cannot handle " + classQuantity + " for class " + valueClass.getSimpleName());
                        br.close();
                }
            } //
            //
            // PathUtm.class
            //
            else if (valueClass == PathUtm.class) {
                boolean foundPathStartBracket = false, foundPathEndBracket = false;
                boolean foundListStartBracket = false, foundListEndBracket = false;
                ArrayList<Location> locations = new ArrayList<Location>();
                ArrayList<PathUtm> paths = new ArrayList<PathUtm>();
                switch (classQuantity) {
                    case SINGLE:
                        try {
                            String line = br.readLine();
                            while (line != null) {
                                if (line.trim().startsWith("#")) {
                                    // Comment line
                                    line = br.readLine();
                                    continue;
                                }
                                if (!foundPathStartBracket) {
                                    if (line.startsWith("[")) {
                                        foundPathStartBracket = true;
                                    } else {
                                        LOGGER.severe("Expected '[': Failed to process line from value import: \"" + line + "\"");
                                    }
                                } else if (!foundPathEndBracket) {
                                    if (line.startsWith("]")) {
                                        return new PathUtm(locations);
                                    } else {
                                        int split = line.indexOf(",");
                                        if (split == -1) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                        String lat = line.substring(0, split);
                                        String lon = line.substring(split + 1);
                                        try {
                                            double pointLat = new Double(lat).doubleValue();
                                            double pointLon = new Double(lon).doubleValue();
                                            LatLon latLon = new LatLon(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon));
                                            Location location2 = Conversion.latLonToLocation(latLon);
                                            if (location2 != null) {
                                                locations.add(location2);
                                            }
                                        } catch (NumberFormatException ex) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                    }
                                }
                                line = br.readLine();
                            }
                        } finally {
                            br.close();
                        }
                        break;
                    case ARRAY_LIST:
                        try {
                            String line = br.readLine();
                            while (line != null) {
                                if (line.trim().startsWith("#")) {
                                    // Comment line
                                    line = br.readLine();
                                    continue;
                                }
                                if (!foundListStartBracket) {
                                    // Haven't started list yet
                                    if (line.startsWith("[")) {
                                        foundListStartBracket = true;
                                    } else {
                                        LOGGER.severe("Expected '[': Failed to process line from value import: \"" + line + "\"");
                                    }
                                } else if (!foundPathStartBracket) {
                                    // Haven't started path yet
                                    if (line.startsWith("[")) {
                                        foundPathStartBracket = true;
                                    } else if (line.startsWith("]")) {
                                        // End list
                                        return paths;
                                    } else {
                                        LOGGER.severe("Expected '[': Failed to process line from value import: \"" + line + "\"");
                                    }
                                } else if (!foundListEndBracket) {
                                    // Haven't ended list yet
                                    if (line.startsWith("]")) {
                                        if (!foundPathEndBracket) {
                                            // Add path to list, reset for the next path
                                            paths.add(new PathUtm(locations));
                                            foundPathEndBracket = false;
                                            foundPathStartBracket = false;
                                            locations = new ArrayList<Location>();
                                        } else {
                                            // End list
                                            return paths;
                                        }
                                    } else {
                                        // Try to add location entry to current path
                                        int split = line.indexOf(",");
                                        if (split == -1) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                        String lat = line.substring(0, split);
                                        String lon = line.substring(split + 1);
                                        try {
                                            double pointLat = new Double(lat).doubleValue();
                                            double pointLon = new Double(lon).doubleValue();
                                            LatLon latLon = new LatLon(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon));
                                            Location location2 = Conversion.latLonToLocation(latLon);
                                            if (location2 != null) {
                                                locations.add(location2);
                                            }
                                        } catch (NumberFormatException ex) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                    }
                                }
                                line = br.readLine();
                            }
                        } finally {
                            br.close();
                        }
                        break;
                    default:
                        LOGGER.severe("Cannot handle " + classQuantity + " for class " + valueClass.getSimpleName());
                        br.close();
                }
            } //
            //
            // Area2D.class
            //
            else if (valueClass == Area2D.class) {
                boolean foundAreaStartBracket = false, foundAreaEndBracket = false;
                boolean foundListStartBracket = false, foundListEndBracket = false;
                ArrayList<Location> locations = new ArrayList<Location>();
                ArrayList<Area2D> areas = new ArrayList<Area2D>();
                switch (classQuantity) {
                    case SINGLE:
                        try {
                            String line = br.readLine();
                            while (line != null) {
                                if (line.trim().startsWith("#")) {
                                    // Comment line
                                    line = br.readLine();
                                    continue;
                                }
                                if (!foundAreaStartBracket) {
                                    if (line.startsWith("[")) {
                                        foundAreaStartBracket = true;
                                    } else {
                                        LOGGER.severe("Expected '[': Failed to process line from value import: \"" + line + "\"");
                                    }
                                } else if (!foundAreaEndBracket) {
                                    if (line.startsWith("]")) {
                                        // Finish area
                                        return new Area2D(locations);
                                    } else {
                                        int split = line.indexOf(",");
                                        if (split == -1) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                        String lat = line.substring(0, split);
                                        String lon = line.substring(split + 1);
                                        try {
                                            double pointLat = new Double(lat).doubleValue();
                                            double pointLon = new Double(lon).doubleValue();
                                            LatLon latLon = new LatLon(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon));
                                            Location location2 = Conversion.latLonToLocation(latLon);
                                            if (location2 != null) {
                                                locations.add(location2);
                                            }
                                        } catch (NumberFormatException ex) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                    }
                                }
                                line = br.readLine();
                            }
                        } finally {
                            br.close();
                        }
                        break;
                    case ARRAY_LIST:
                        try {
                            String line = br.readLine();
                            while (line != null) {
                                if (line.trim().startsWith("#")) {
                                    // Comment line
                                    line = br.readLine();
                                    continue;
                                }
                                if (!foundListStartBracket) {
                                    // Haven't started list yet
                                    if (line.startsWith("[")) {
                                        foundListStartBracket = true;
                                    } else {
                                        LOGGER.severe("Expected '[': Failed to process line from value import: \"" + line + "\"");
                                    }
                                } else if (!foundAreaStartBracket) {
                                    // Haven't started path yet
                                    if (line.startsWith("[")) {
                                        foundAreaStartBracket = true;
                                    } else if (line.startsWith("]")) {
                                        // End list
                                        return areas;
                                    } else {
                                        LOGGER.severe("Expected '[': Failed to process line from value import: \"" + line + "\"");
                                    }
                                } else if (!foundListEndBracket) {
                                    // Haven't ended list yet
                                    if (line.startsWith("]")) {
                                        if (!foundAreaEndBracket) {
                                            // Add area to list, reset for the next area
                                            areas.add(new Area2D(locations));
                                            foundAreaEndBracket = false;
                                            foundAreaStartBracket = false;
                                            locations = new ArrayList<Location>();
                                        } else {
                                            // End list
                                            return areas;
                                        }
                                    } else {
                                        // Try to add location entry to current area
                                        int split = line.indexOf(",");
                                        if (split == -1) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                        String lat = line.substring(0, split);
                                        String lon = line.substring(split + 1);
                                        try {
                                            double pointLat = new Double(lat).doubleValue();
                                            double pointLon = new Double(lon).doubleValue();
                                            LatLon latLon = new LatLon(Angle.fromDegreesLatitude(pointLat), Angle.fromDegreesLongitude(pointLon));
                                            Location location2 = Conversion.latLonToLocation(latLon);
                                            if (location2 != null) {
                                                locations.add(location2);
                                            }
                                        } catch (NumberFormatException ex) {
                                            LOGGER.severe("Failed to process line from value import: \"" + line + "\"");
                                            line = br.readLine();
                                            continue;
                                        }
                                    }
                                }
                                line = br.readLine();
                            }
                        } finally {
                            br.close();
                        }
                        break;
                    default:
                        LOGGER.severe("Cannot handle " + classQuantity + " for class " + valueClass.getSimpleName());
                        br.close();
                }
            } // Unhandled class
            else {
                LOGGER.severe("Cannot import values of class " + valueClass.getSimpleName());
                br.close();
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        LOGGER.severe("Value import failed, returning NULL");
        return null;
    }
}
