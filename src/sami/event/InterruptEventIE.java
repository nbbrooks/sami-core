/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class InterruptEventIE extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
        // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public InterruptEventIE() {
        id = UUID.randomUUID();
    }

    public InterruptEventIE(UUID missionId) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }
    
}
