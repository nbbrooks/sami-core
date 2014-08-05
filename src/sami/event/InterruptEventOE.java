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
public class InterruptEventOE extends OutputEvent{
    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields

    public InterruptEventOE() {
        id = UUID.randomUUID();
    }

    public String toString() {
        return "InterruptEventOE";
    }
}
