package sami;

import java.security.AccessControlException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author nbb
 */
public class ResetPreferences {

    private static final Logger LOGGER = Logger.getLogger(ResetPreferences.class.getName());

    public static void main(String[] args) {

        try {
            Preferences p = Preferences.userRoot();
            if (p == null) {
                LOGGER.severe("Java preferences file is NULL");
            } else {
                p.put("LAST_DRM_NAME", "/Users/nbb/Code/sami-release/sami-crw/config/crw.drm");
                p.put("LAST_DRM_FOLDER", "");
                p.put("LAST_EPF_NAME", "");
                p.put("LAST_EPF_FOLDER", "");
                p.put("LAST_DREAAM_DCF_NAME", "");
                p.put("LAST_DREAAM_DCF_FOLDER", "");
                p.put("LAST_DREAAM_CFG_FOLDER", "");
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Preferences.userRoot access control exception: " + e.toString());
        }
    }
}
