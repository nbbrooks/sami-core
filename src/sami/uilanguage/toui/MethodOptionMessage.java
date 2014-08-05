/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sami.uilanguage.toui;

import java.util.List;
import java.util.UUID;

/**
 *
 * @author Nicolò Marchi <marchi.nicolo@gmail.com>
 */
public class MethodOptionMessage extends SelectionMessage {

    public MethodOptionMessage(UUID relevantOutputEventId, UUID missionId, int priority, boolean allowMultiple, List<?> optionsList) {
        super(relevantOutputEventId, missionId, priority, allowMultiple, false, true, optionsList);
    }
    
}
