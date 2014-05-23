package sami.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.markup.Markup;

/**
 *
 * @author pscerri
 */
public class Event {

    public static final String NONE = "@None";

    private ArrayList<Markup> markups = new ArrayList<Markup>();
    protected UUID missionId;
    protected UUID id;
    final protected HashMap<String, Field> readVariableToField = new HashMap<String, Field>();

    public Event() {
    }

    public void setMissionId(UUID missionId) {
        this.missionId = missionId;
    }

    public UUID getMissionId() {
        return missionId;
    }

    public ArrayList<Markup> getMarkups() {
        return markups;
    }

    public void setMarkups(ArrayList<Markup> markups) {
        this.markups = markups;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public boolean isRelevantAll() {
        return false;
    }

    public boolean getFillAtPlace() {
        // Whether missing parameters for this event should be filled when the plan reaches the Place the event is on (true), or when the plan is loaded (false)
        return false;
    }

    public void addReadVariable(String variableName, Field field) {
        readVariableToField.put(variableName, field);
    }

    public Object deepCopy() {
        Event copy;
        try {
            copy = (Event) this.getClass().newInstance();
            for (Markup markup : markups) {
                copy.markups.add(markup.copy());
            }
            copy.id = id;
            copy.missionId = missionId;
            for (String key : readVariableToField.keySet()) {
                copy.readVariableToField.put(key, readVariableToField.get(key));
            }
            return copy;
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            return null;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
