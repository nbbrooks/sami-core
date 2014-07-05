package sami.engine;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import com.perc.mitpas.adi.mission.planning.task.Task;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.event.AbortMission;
import sami.event.AbortMissionReceived;
import sami.event.GeneratedEventListenerInt;
import sami.event.InputEvent;
import sami.event.MissingParamsReceived;
import sami.event.MissingParamsRequest;
import sami.event.OutputEvent;
import sami.event.ReflectedEventSpecification;
import sami.event.ReflectionHelper;
import sami.handler.EventHandlerInt;
import sami.mission.InEdge;
import sami.mission.InTokenRequirement;
import sami.mission.MissionPlanSpecification;
import sami.mission.OutEdge;
import sami.mission.OutTokenRequirement;
import sami.mission.Place;
import sami.mission.TaskSpecification;
import sami.mission.Token;
import sami.mission.Token.TokenType;
import sami.mission.TokenRequirement;
import sami.mission.Transition;
import sami.mission.Vertex;
import sami.mission.Vertex.FunctionMode;
import sami.proxy.ProxyInt;

/**
 *
 * A PlanManager looks after a single MissionModel Basically the code here is
 * responsible for executing a PetriNet Mostly it is now only capable of
 * executing a State Machine
 *
 * @todo Do something when PlanManager ends, including informing super-mission
 *
 * @author pscerri
 */
public class PlanManager implements GeneratedEventListenerInt, PlanManagerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(PlanManager.class.getName());
    // Variable for counting how many things the operator still needs to fill in before execution
    int repliesExpected = 0;
    // Blocking queue of generated input events waiting to be matched to a parameter input event
    private ArrayBlockingQueue<InputEvent> generatorEventQueue = new ArrayBlockingQueue<InputEvent>(20);
    ArrayList<InputEvent> activeInputEvents = new ArrayList<InputEvent>();
    final ArrayList<Place> placesBeingEntered = new ArrayList<Place>();
    // List of tokens on the mission spec's edges to be used as the default list of tokens to put in the starting place
    //  Null PROXY and ALL tokens are not added to this list
    private ArrayList<Token> startingTokens = new ArrayList<Token>();
    // Lookup table used for retrieving task based tokens (ie for updating a token after a resource allocation is received)
    private HashMap<String, Task> taskNameToTask = new HashMap<String, Task>();
    private HashMap<ITask, Token> taskToToken = new HashMap<ITask, Token>();
    private HashMap<TaskSpecification, Token> taskSpecToToken = new HashMap<TaskSpecification, Token>();
    // Keeps track of variables coming in from InputEvents, to be used in OutputEvents
    private HashMap<String, Object> variableNameToValue = new HashMap<String, Object>();
    // Lookup table used during processing of generated and updated parameter input events
    private HashMap<InputEvent, Transition> inputEventToTransitionMap = new HashMap<InputEvent, Transition>();
    // Lookup table used when submissions are completed
    private HashMap<PlanManager, Place> planManagerToPlace = new HashMap<PlanManager, Place>();
    private HashMap<Place, ArrayList<PlanManager>> placeToActivePlanManagers = new HashMap<Place, ArrayList<PlanManager>>();
    // Lookup for cloned blocking IEs
    private HashMap<InputEvent, HashMap<ProxyInt, InputEvent>> clonedIeTable = new HashMap<InputEvent, HashMap<ProxyInt, InputEvent>>();
    final MissionPlanSpecification mSpec;
    // The model being managed by this PlanManager
    private Place startPlace;
    private String planName;
    public final UUID missionId;
    // Logging levels
    final Level CHECK_T_LVL = Level.FINE;
    final Level EXE_T_LVL = Level.FINE;

    public PlanManager(final MissionPlanSpecification mSpec, UUID missionId, String planName, ArrayList<Token> startingTokens) {
        LOGGER.info("Creating PlanManager for mSpec " + mSpec + " with mission ID " + missionId + " and planName " + planName);
        this.mSpec = mSpec;
        this.missionId = missionId;
        this.planName = planName;
        this.startingTokens = startingTokens;
        startPlace = mSpec.getUninstantiatedStart();

        // Create task tokens
        LOGGER.info("Creating task tokens for task specifications");
        for (TaskSpecification taskSpec : mSpec.getTaskSpecList()) {
            Token taskToken = createToken(taskSpec);
            LOGGER.info("\t" + taskSpec + " -> " + taskToken);
            this.startingTokens.add(taskToken);
        }

        // If there are any parameters on the events that need to be filled in, request from the operator
        ArrayList<ReflectedEventSpecification> editableEventSpecs = mSpec.getEventSpecsRequestingParams();
        if (editableEventSpecs.size() > 0) {
            LOGGER.fine("Missing/editable parameters in eventSpecs: " + editableEventSpecs);

            // Create vertices to get missing parameters
            Place missingParamsPlace = new Place("Get Params", FunctionMode.Nominal);
            Transition missingParamsTransition = new Transition("Got Params", FunctionMode.Nominal);
            InEdge edge1 = new InEdge(missingParamsPlace, missingParamsTransition, FunctionMode.Nominal);
            OutEdge edge2 = new OutEdge(missingParamsTransition, startPlace, FunctionMode.Nominal);
            // Add vetices to plan
            missingParamsPlace.addOutTransition(missingParamsTransition);
            missingParamsTransition.addInPlace(missingParamsPlace);
            missingParamsTransition.addOutPlace(startPlace);
            edge1.addTokenRequirement(new InTokenRequirement(TokenRequirement.MatchCriteria.None, null));
            edge2.addTokenRequirement(new OutTokenRequirement(TokenRequirement.MatchCriteria.AnyToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take));
            missingParamsPlace.addOutEdge(edge1);
            missingParamsTransition.addInEdge(edge1);
            missingParamsTransition.addOutEdge(edge2);
            startPlace.addInEdge(edge2);
            startPlace = missingParamsPlace;

            // Make list of missing/editable parameter fields
            Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions = new Hashtable<ReflectedEventSpecification, Hashtable<Field, String>>();
            Hashtable<ReflectedEventSpecification, ArrayList<Field>> eventSpecToFields = new Hashtable<ReflectedEventSpecification, ArrayList<Field>>();
            for (ReflectedEventSpecification eventSpec : editableEventSpecs) {
                LOGGER.fine("Event spec for " + eventSpec.getClassName() + " has missing/editable fields");
                // Hashtable entries for this eventSpec
                Hashtable<Field, String> fieldDescriptions = new Hashtable<Field, String>();
                ArrayList<Field> fields = new ArrayList<Field>();
                eventSpecToFieldDescriptions.put(eventSpec, fieldDescriptions);
                eventSpecToFields.put(eventSpec, fields);
                // Defined but editable values
                HashMap<String, Object> fieldNameToValue = eventSpec.getFieldValues();
                HashMap<String, String> fieldNameToReadVariable = eventSpec.getReadVariables();
                HashMap<String, Boolean> fieldNameToEditable = eventSpec.getEditableFields();

                int missingCount = 0, editableCount = 0;
                try {
                    Class eventClass = Class.forName(eventSpec.getClassName());
                    ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
                    for (String fieldName : fieldNames) {
                        LOGGER.fine("\tField: " + fieldName + " = " + fieldNameToValue.get(fieldName));
                        if (!fieldNameToValue.containsKey(fieldName)
                                && !fieldNameToReadVariable.containsKey(fieldName)) {
                            LOGGER.finer("\t\t Missing");
                            try {
                                Class eventSpecClass = Class.forName(eventSpec.getClassName());
                                Field missingField = ReflectionHelper.getField(eventSpecClass, fieldName);
                                if (missingField != null) {
                                    missingCount++;
                                    fieldDescriptions.put(missingField, "");
                                    fields.add(missingField);
                                } else {
                                    LOGGER.severe("Could not find field \"" + fieldName + "\" in class " + eventSpecClass.getSimpleName() + " or any super class");
                                }
                            } catch (ClassNotFoundException cnfe) {
                                cnfe.printStackTrace();
                            }
                        } else if (fieldNameToEditable.containsKey(fieldName)
                                && fieldNameToEditable.get(fieldName)) {
                            LOGGER.finer("\t\t Editable");
                            editableCount++;
                            try {
                                Class eventSpecClass = Class.forName(eventSpec.getClassName());
                                Field editableField = ReflectionHelper.getField(eventSpecClass, fieldName);
                                if (editableField != null) {
                                    fieldDescriptions.put(editableField, "");
                                    fields.add(editableField);
                                } else {
                                    LOGGER.severe("Could not find field \"" + fieldName + "\" in class " + eventSpecClass.getSimpleName() + " or any super class");
                                }
                            } catch (ClassNotFoundException cnfe) {
                                cnfe.printStackTrace();
                            }
                        } else if (!fieldNameToEditable.containsKey(fieldName)) {
                            LOGGER.severe("\t\tNo entry in fieldNameToEditable");
                        } else {
                            LOGGER.finer("\t\t Locked");
                        }

                        if (fieldNameToValue.get(fieldName) == null
                                && !fieldNameToEditable.get(fieldName)) {
                            LOGGER.severe("Have a non-editable field: " + fieldName + " with no value!");
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                } catch (NoSuchFieldException ex) {
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
                LOGGER.fine("\t Have " + missingCount + " missing fields");
                LOGGER.fine("\t Have " + editableCount + " editable fields");
            }

            // Create events to get missing parameters
            //@todo Modify constructors?
            MissingParamsRequest request = new MissingParamsRequest(missionId, eventSpecToFieldDescriptions);
            MissingParamsReceived response = new MissingParamsReceived();
            request.setMissionId(missionId);
            response.setMissionId(missionId);
            response.setRelevantOutputEventId(request.getId());
            missingParamsPlace.addOutputEvent(request);
            missingParamsTransition.addInputEvent(response);

            // Add abort mission handling
            // Add transition
            Transition abortTransition = new Transition("AbortMission", FunctionMode.Recovery);
            AbortMissionReceived abortReceived = new AbortMissionReceived(missionId);
            abortTransition.addInputEvent(abortReceived);
            // Add end place with AbortMission
            Place abortPlace = new Place("AbortMission", FunctionMode.Recovery);
            abortPlace.setIsEnd(true);
            AbortMission abortMission = new AbortMission(missionId);
            abortPlace.addOutputEvent(abortMission);
            // Add edges
            InEdge abortInEdge = new InEdge(missingParamsPlace, abortTransition, FunctionMode.Recovery);
            abortInEdge.addTokenRequirement(new InTokenRequirement(TokenRequirement.MatchCriteria.None, null));
            abortTransition.addInEdge(abortInEdge);
            missingParamsPlace.addOutEdge(abortInEdge);
            abortTransition.addInPlace(missingParamsPlace);
            missingParamsPlace.addOutTransition(abortTransition);
            OutEdge abortOutEdge = new OutEdge(abortTransition, abortPlace, FunctionMode.Recovery);
            abortOutEdge.addTokenRequirement(new OutTokenRequirement(TokenRequirement.MatchCriteria.AnyToken, TokenRequirement.MatchQuantity.All, TokenRequirement.MatchAction.Take));
            abortTransition.addOutEdge(abortOutEdge);
            abortPlace.addInEdge(abortOutEdge);
            abortTransition.addOutPlace(abortPlace);
            abortPlace.addInTransition(abortTransition);
        } else {
            LOGGER.info("No missing params, instantiating plan");
            if (!mSpec.isInstantiated()) {
                mSpec.instantiate(missionId);
            }
        }

        generatedEventThread.start();
    }

    Thread generatedEventThread = new Thread() {
        public void run() {
            while (true) {
                try {
                    InputEvent generatedEvent = generatorEventQueue.take();
                    processGeneratedEvent(generatedEvent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * Begin the execution of the plan
     *
     * Either wait for a proxy token to be added, or use a "mission token",
     * i.e., one that is not associated with any proxy
     *
     * @param plan
     */
    public void start() {
        LOGGER.log(Level.FINE, "Begin plan start");

        while (!generatedEventThread.isAlive()) {
            LOGGER.log(Level.WARNING, "generatedEventThread is not alive, sleeping for 1s");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        boolean checkedForTransition = false;
        // Add in any starting tokens to the start place
        LOGGER.log(Level.FINE, "\tAdding received token list " + startingTokens + " to start place " + startPlace);
        enterPlace(startPlace, startingTokens, true);
        checkedForTransition = true;

        if (!checkedForTransition) {
            for (Transition transition : startPlace.getOutTransitions()) {
                boolean execute = checkTransition(transition);
                if (execute) {
                    executeTransition(transition);
                };
            }
        }

        LOGGER.log(Level.FINE, "Plan has finished starting");
        Engine.getInstance().started(this);
    }

    private synchronized boolean checkTransition(Transition transition) {
        LOGGER.log(CHECK_T_LVL, "Checking " + transition);

        boolean failure = false;

        ////
        // Check that each InputEvent has occurred
        ////
        ArrayList<InputEvent> inputEvents = transition.getInputEvents();
        LOGGER.log(CHECK_T_LVL, "\tChecking for input events: " + inputEvents);
        for (InputEvent ie : inputEvents) {
            if (!transition.getInputEventStatus(ie)) {
                LOGGER.log(CHECK_T_LVL, "\t\tInput event " + ie + " is not ready");
                failure = true;
                break;
            } else {
                LOGGER.log(CHECK_T_LVL, "\t\tInput event " + ie + " is ready");
            }
        }

        if (!failure) {
            ////
            // Check the token requirements of each incoming edge
            ////
            check:
            for (InEdge inEdge : transition.getInEdges()) {
                // Get incoming Place
                Place inPlace = inEdge.getStart();

                ////
                // Check that if there is a sub-mission that it has completed
                ////
                boolean allSubMFinished = !placeToActivePlanManagers.containsKey(inPlace) || (placeToActivePlanManagers.containsKey(inPlace) && placeToActivePlanManagers.get(inPlace).isEmpty());
                if (!allSubMFinished) {
                    LOGGER.log(CHECK_T_LVL, "\tSub-missions " + placeToActivePlanManagers.get(inPlace) + " on " + inPlace + " are not yet complete");
                    failure = true;
                    break check;
                } else if (allSubMFinished && placeToActivePlanManagers.containsKey(inPlace)) {
                    LOGGER.log(CHECK_T_LVL, "\tSub-missions on " + inPlace + " are complete");
                }

                ////
                // Check edge requirements
                ////
                ArrayList<Token> placeTokens = (ArrayList<Token>) inPlace.getTokens();
                LOGGER.log(CHECK_T_LVL, "\tChecking " + inEdge + " with in reqs [" + inEdge.getTokenRequirements() + "] against in " + inPlace + " with [" + placeTokens + "]");
                for (TokenRequirement inReq : inEdge.getTokenRequirements()) {
                    LOGGER.log(CHECK_T_LVL, "\t\tChecking in req: " + inReq + " and incoming place with tokens: " + placeTokens);
                    if (inReq.getMatchQuantity() == TokenRequirement.MatchQuantity.Number && inReq.getQuantity() == 0) {
                        LOGGER.severe("\t\t\tMatch number quantity is zero, ignoring token requirement: " + inReq);
                        continue;
                    } else if (inReq.getMatchQuantity() == TokenRequirement.MatchQuantity.Number && inReq.getQuantity() < 0) {
                        LOGGER.severe("\t\t\tMatch number quantity is negative, ignoring token requirement: " + inReq);
                        continue;
                    }
                    switch (inReq.getMatchCriteria()) {
                        case AnyProxy:
                            switch (inReq.getMatchQuantity()) {
                                case None:
                                    // Check that there are no proxy tokens
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Proxy) {
                                            failure = true;
                                            break;
                                        }
                                    }
                                    break;
                                case Number:
                                    // Check that there are at least n proxy tokens
                                    int proxyTokenCount = 0;
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Proxy) {
                                            proxyTokenCount++;
                                            if (proxyTokenCount >= inReq.getQuantity()) {
                                                break;
                                            }
                                        }
                                    }
                                    if (proxyTokenCount < inReq.getQuantity()) {
                                        failure = true;
                                    }
                                    break;
                                default:
                                    LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + inReq);
                                    failure = true;
                                    break;
                            }
                            break;
                        case AnyTask:
                            switch (inReq.getMatchQuantity()) {
                                case None:
                                    // Check that there are no task tokens
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Task) {
                                            failure = true;
                                            break;
                                        }
                                    }
                                    break;
                                case Number:
                                    // Check that there are at least n task tokens
                                    int taskTokenCount = 0;
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Task) {
                                            taskTokenCount++;
                                            if (taskTokenCount >= inReq.getQuantity()) {
                                                break;
                                            }
                                        }
                                    }
                                    if (taskTokenCount < inReq.getQuantity()) {
                                        failure = true;
                                    }
                                    break;
                                default:
                                    LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + inReq);
                                    failure = true;
                                    break;
                            }
                            break;
                        case AnyToken:
                            switch (inReq.getMatchQuantity()) {
                                case None:
                                    // Check that there are no tokens
                                    if (!placeTokens.isEmpty()) {
                                        failure = true;
                                    }
                                    break;
                                case Number:
                                    // Check that there are at least n tokens
                                    if (placeTokens.size() < inReq.getQuantity()) {
                                        failure = true;
                                    }
                                    break;
                                default:
                                    LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + inReq);
                                    failure = true;
                                    break;
                            }
                            break;
                        case Generic:
                            switch (inReq.getMatchQuantity()) {
                                case None:
                                    // Check that there are no generic tokens
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Generic) {
                                            failure = true;
                                            break;
                                        }
                                    }
                                    break;
                                case Number:
                                    // Check that there are at least n generic tokens
                                    int genericTokenCount = 0;
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Generic) {
                                            genericTokenCount++;
                                            if (genericTokenCount >= inReq.getQuantity()) {
                                                break;
                                            }
                                        }
                                    }
                                    if (genericTokenCount < inReq.getQuantity()) {
                                        failure = true;
                                    }
                                    break;
                                default:
                                    LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + inReq);
                                    failure = true;
                                    break;
                            }
                            break;
                        case None:
                            break;
                        case RelevantToken:
                            // A relevant token is a token whose proxy (non-null) is contained in an input event's relevantProxyList
                            ArrayList<ProxyInt> relevantProxies;
                            switch (inReq.getMatchQuantity()) {
                                case None:
                                    // Check that there are no copies of any of the relevant proxy tokens
                                    relevantProxies = getRelevantProxies(transition);
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Proxy
                                                && placeToken.getProxy() != null
                                                && relevantProxies.contains(placeToken.getProxy())) {
                                            failure = true;
                                            break;
                                        }
                                    }
                                    break;
                                case Number:
                                    // Check that there are at least n of the relevant tokens
                                    int count = 0;
                                    relevantProxies = getRelevantProxies(transition);
                                    // Go through token list and remove item from relevant proxy list where appropriate 
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Proxy
                                                && placeToken.getProxy() != null
                                                && relevantProxies.contains(placeToken.getProxy())) {
                                            count++;
                                            if (count >= inReq.getQuantity()) {
                                                break;
                                            }
                                        }
                                    }
                                    if (count < inReq.getQuantity()) {
                                        failure = true;
                                    }
                                    break;
                                case All:
                                    // Check that there is at least n copies of each relevant proxy token
                                    // Get cloned list of relevant proxies
                                    relevantProxies = (ArrayList<ProxyInt>) (getRelevantProxies(transition).clone());
                                    // Go through token list and remove item from relevant proxy list where appropriate 
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Proxy
                                                && placeToken.getProxy() != null
                                                && relevantProxies.contains(placeToken.getProxy())) {
                                            relevantProxies.remove(placeToken.getProxy());
                                            if (relevantProxies.isEmpty()) {
                                                break;
                                            }
                                        }
                                    }
                                    // If anything is still left in the relevant proxies list, the check fails
                                    if (!relevantProxies.isEmpty()) {
                                        failure = true;
                                    }
                                    break;
                                default:
                                    LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + inReq);
                                    failure = true;
                                    break;
                            }
                            break;
                        case SpecificTask:
                            String taskName = inReq.getTaskName();
                            Task task = taskNameToTask.get(taskName);
                            switch (inReq.getMatchQuantity()) {
                                case None:
                                    // Check that there is no copy of the specific task token
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Task
                                                && placeToken.getTask() != null
                                                && placeToken.getTask() == task) {
                                            failure = true;
                                            break;
                                        }
                                    }
                                    break;
                                case Number:
                                    // Check that there are n copies of the specific task token
                                    int count = 0;
                                    for (Token placeToken : placeTokens) {
                                        if (placeToken.getType() == TokenType.Task
                                                && placeToken.getTask() != null
                                                && placeToken.getTask() == task) {
                                            count++;
                                            if (count >= inReq.getQuantity()) {
                                                break;
                                            }
                                        }
                                    }
                                    if (count < inReq.getQuantity()) {
                                        failure = true;
                                    }
                                    break;
                                default:
                                    LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + inReq);
                                    failure = true;
                                    break;
                            }
                            break;
                        default:
                            LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + inReq);
                            failure = true;
                            break;
                    }
                    if (failure) {
                        LOGGER.log(CHECK_T_LVL, "\t\t\tFailed check");
                        break check;
                    }
                }
                if (failure) {
                    LOGGER.severe("Logic error - I should not be here");
                }
                LOGGER.log(CHECK_T_LVL, "\t\tEdge requirements have been met");
            }
        }

        if (!failure) {
            LOGGER.log(CHECK_T_LVL, "\tTransition " + transition + " is ready!");
            return true;
        }
        return false;
    }

    private synchronized void executeTransition(Transition transition) {
        LOGGER.info("Executing " + transition + ", inPlaces: " + transition.getInPlaces() + ", inEdges: " + transition.getInEdges() + ", outPlaces: " + transition.getOutPlaces() + ", outEdges: " + transition.getOutEdges());

        synchronized (placesBeingEntered) {
            for (Place place : transition.getInPlaces()) {
                if (placesBeingEntered.contains(place)) {
                    LOGGER.log(EXE_T_LVL, "\tAborting executeTransition becaues an incoming place is still being entered: " + place);
                    return;
                }
            }
        }

        ////
        // Figure out what to add and remove
        ////
        // For each outgoing place
        // For each out place, which tokens from in places should be added to the out place?
        Hashtable<Place, Hashtable<Place, boolean[]>> outPlaceToInPlaceToTokenAdd = new Hashtable<Place, Hashtable<Place, boolean[]>>();
        // For each in place, which tokens should be removed?
        Hashtable<Place, boolean[]> inPlaceToTokenRemove = new Hashtable<Place, boolean[]>();
        // For each out place, which tokens from input event's relevant token list should be added to the out place?
        Hashtable<Place, Hashtable<InputEvent, boolean[]>> outPlaceToIeToRelTokenAdd = new Hashtable<Place, Hashtable<InputEvent, boolean[]>>();
        // For each out place, which specifically named task tokens should be added?
        Hashtable<Place, ArrayList<Token>> outPlaceToTaskTokensToAdd = new Hashtable<Place, ArrayList<Token>>();
        // For each out place, how many generic tokens should be added?
        Hashtable<Place, Integer> outPlaceToGenericTokenAdd = new Hashtable<Place, Integer>();

        // Initialize hashtable values
        Hashtable<InputEvent, boolean[]> ieToRelTokenAddMaster = new Hashtable<InputEvent, boolean[]>();
        for (InputEvent ie : transition.getInputEvents()) {
            if (ie.getGeneratorEvent() == null) {
                LOGGER.log(EXE_T_LVL, "\tie: " + ie + "\tgeneratorEvent is null: should be a null RP blocking IE: param IE RP = " + ie.getRelevantProxyList());
            }
            if (ie.getGeneratorEvent() != null && ie.getGeneratorEvent().getRelevantProxyList() != null) {
                LOGGER.log(EXE_T_LVL, "\tie: " + ie + "\tgeneratorEvent is not null and has RP: " + ie.getGeneratorEvent().getRelevantProxyList());
                ieToRelTokenAddMaster.put(ie.getGeneratorEvent(), new boolean[ie.getGeneratorEvent().getRelevantProxyList().size()]);
            }
        }
        for (Place inPlace : transition.getInPlaces()) {
            inPlaceToTokenRemove.put(inPlace, new boolean[inPlace.getTokens().size()]);
        }
        for (Place outPlace : transition.getOutPlaces()) {
            Hashtable<Place, boolean[]> inPlaceToTokenAdd = new Hashtable<Place, boolean[]>();
            for (Place inPlace : transition.getInPlaces()) {
                inPlaceToTokenAdd.put(inPlace, new boolean[inPlace.getTokens().size()]);
            }
            outPlaceToInPlaceToTokenAdd.put(outPlace, inPlaceToTokenAdd);
            outPlaceToIeToRelTokenAdd.put(outPlace, (Hashtable<InputEvent, boolean[]>) ieToRelTokenAddMaster.clone());
            outPlaceToTaskTokensToAdd.put(outPlace, new ArrayList<Token>());
            outPlaceToGenericTokenAdd.put(outPlace, 0);
        }

        // Start filling hashtable values
        for (OutEdge outEdge : transition.getOutEdges()) {
            LOGGER.log(EXE_T_LVL, "\tChecking " + outEdge + " with out reqs " + outEdge.getTokenRequirements());
            // Get outgoing place
            Place outPlace = outEdge.getEnd();
            Hashtable<Place, boolean[]> inPlaceToTokenAdd = outPlaceToInPlaceToTokenAdd.get(outPlace);

            for (OutTokenRequirement outReq : outEdge.getTokenRequirements()) {
                LOGGER.log(EXE_T_LVL, "\t\tChecking " + outReq);
                if (outReq.getMatchQuantity() == TokenRequirement.MatchQuantity.Number && outReq.getQuantity() == 0) {
                    LOGGER.severe("\t\t\tMatch number quantity is zero, ignoring token requirement: " + outReq);
                    continue;
                } else if (outReq.getMatchQuantity() == TokenRequirement.MatchQuantity.Number && outReq.getQuantity() < 0) {
                    LOGGER.severe("\t\t\tMatch number quantity is negative, ignoring token requirement: " + outReq);
                    continue;
                }
                switch (outReq.getMatchCriteria()) {
                    case AnyProxy:
                        switch (outReq.getMatchAction()) {
                            case Add:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Add proxy tokens in all incoming places (including duplicates)
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Proxy) {
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Add n proxy tokens chosen arbitrarily from incoming places (does not guarantee unique proxies)
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Proxy) {
                                                    tokenAdd[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Consume:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all proxy tokens in all incoming places
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Proxy) {
                                                    tokenRemove[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n proxy tokens chosen arbitrarily from incoming places (does not guarantee unique proxies)
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Proxy) {
                                                    tokenRemove[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Take:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove proxy tokens in all incoming places (including duplicates) and add them to the outgoing place
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Proxy) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n proxy tokens chosen arbitrarily from incoming places (does not guarantee unique proxies) and add them to the outgoing place
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Proxy) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            default:
                                LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                break;
                        }
                        break;
                    case AnyTask:
                        switch (outReq.getMatchAction()) {
                            case Add:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Add all task tokens in all incoming places (including duplicates)
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Proxy) {
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Add n task tokens chosen arbitrarily from incoming places (does not guarantee unique tasks)
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Task) {
                                                    tokenAdd[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Consume:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all task tokens in all incoming places
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Task) {
                                                    tokenRemove[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n task tokens chosen arbitrarily from incoming places (does not guarantee unique tasks)
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Task) {
                                                    tokenRemove[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Take:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all task tokens in all incoming places (including duplicates) and add them to the outgoing place
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Task) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n task tokens chosen arbitrarily from incoming places (does not guarantee unique tasks) and add them to the outgoing place
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Task) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            default:
                                LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                break;
                        }
                        break;
                    case AnyToken:
                        switch (outReq.getMatchAction()) {
                            case Add:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Add all tokens in all incoming places (including duplicates)
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                tokenAdd[i] = true;
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Add n tokens chosen arbitrarily from incoming places (does not guarantee unique tokens)
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                tokenAdd[i] = true;
                                                count++;
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Consume:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all tokens in all incoming places
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                tokenRemove[i] = true;
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n tokens chosen arbitrarily from incoming places (does not guarantee unique tokens)
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                tokenRemove[i] = true;
                                                count++;
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Take:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all tokens in all incoming places (including duplicates) and add them to the outgoing place
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                tokenRemove[i] = true;
                                                tokenAdd[i] = true;
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n tokens chosen arbitrarily from incoming places (does not guarantee unique tokens) and add them to the outgoing place
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                tokenRemove[i] = true;
                                                tokenAdd[i] = true;
                                                count++;
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            default:
                                LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                break;
                        }
                        break;
                    case Generic:
                        switch (outReq.getMatchAction()) {
                            case Add:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Add all generic tokens found in incoming places
                                        for (Place inPlace : inPlaceToTokenAdd.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Generic) {
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Add n generic tokens
                                        outPlaceToGenericTokenAdd.put(outPlace, outReq.getQuantity());
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Consume:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all generic tokens in all incoming places
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Generic) {
                                                    tokenRemove[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n generic tokens chosen arbitrarily from incoming places
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Generic) {
                                                    tokenRemove[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Take:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all generic tokens in all incoming places and add them to the outgoing place
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i).getType() == TokenType.Generic) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n generic tokens chosen arbitrarily from incoming places and add them to the outgoing place
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i).getType() == TokenType.Generic) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            default:
                                LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                break;
                        }
                        break;
                    case None:
                        break;
                    case RelevantToken:
                        // A relevant token is a token whose proxy (non-null) is contained in an input event's relevantProxyList
                        ArrayList<ProxyInt> relevantProxies;
                        Hashtable<InputEvent, boolean[]> ieToRelTokenAdd = outPlaceToIeToRelTokenAdd.get(outPlace);
                        switch (outReq.getMatchAction()) {
                            case Add:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Add a copy of each relevant proxy's proxy token to the outgoing place
                                        for (boolean[] tokenAdd : ieToRelTokenAdd.values()) {
                                            for (int i = 0; i < tokenAdd.length; i++) {
                                                tokenAdd[i] = true;
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Arbitrarily choose n relevant proxys from input events on the transition and add a copy of their proxy token to the outgoing place
                                        int count = 0;
                                        boolean done = false;
                                        for (boolean[] tokenAdd : ieToRelTokenAdd.values()) {
                                            for (int i = 0; i < tokenAdd.length; i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                tokenAdd[i] = true;
                                                count++;
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Consume:
                                relevantProxies = getRelevantProxies(transition);
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove all relevant tokens from all incoming places
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> placeTokens = (ArrayList<Token>) inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < tokenRemove.length; i++) {
                                                Token placeToken = placeTokens.get(i);
                                                if (placeToken.getType() == TokenType.Proxy
                                                        && placeToken.getProxy() != null
                                                        && relevantProxies.contains(placeToken.getProxy())) {
                                                    tokenRemove[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Arbitrarily choose a total of n relevant tokens from incoming places and remove them
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> placeTokens = (ArrayList<Token>) inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            for (int i = 0; i < tokenRemove.length; i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                Token placeToken = placeTokens.get(i);
                                                if (placeToken.getType() == TokenType.Proxy
                                                        && placeToken.getProxy() != null
                                                        && relevantProxies.contains(placeToken.getProxy())) {
                                                    tokenRemove[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Take:
                                relevantProxies = getRelevantProxies(transition);
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // For each relevant proxy add its proxy token to the outgoing place and remove it from all incoming places
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> placeTokens = (ArrayList<Token>) inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < tokenRemove.length; i++) {
                                                Token placeToken = placeTokens.get(i);
                                                if (placeToken.getType() == TokenType.Proxy
                                                        && placeToken.getProxy() != null
                                                        && relevantProxies.contains(placeToken.getProxy())) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Arbitrarily choose n tokens from incoming places with a relevant proxy, remove them, and add them to the outgoing place
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            ArrayList<Token> placeTokens = (ArrayList<Token>) inPlace.getTokens();
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            for (int i = 0; i < tokenRemove.length; i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                Token placeToken = placeTokens.get(i);
                                                if (placeToken.getType() == TokenType.Proxy
                                                        && placeToken.getProxy() != null
                                                        && relevantProxies.contains(placeToken.getProxy())) {
                                                    tokenRemove[i] = true;
                                                    tokenAdd[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            default:
                                LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                break;
                        }
                        break;
                    case SpecificTask:
                        String taskName = outReq.getTaskName();
                        Task task = taskNameToTask.get(taskName);
                        Token taskToken = null;
                        if (task != null) {
                            taskToken = getToken(task);
                        }
                        ArrayList<Token> taskTokensToAdd = outPlaceToTaskTokensToAdd.get(outPlace);
                        switch (outReq.getMatchAction()) {
                            case Add:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Add all copies of the task token found in incoming places
                                        for (Place inPlace : inPlaceToTokenAdd.keySet()) {
                                            boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i) == taskToken) {
                                                    tokenAdd[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Add n copies of the task token
                                        for (int i = 0; i < outReq.getQuantity(); i++) {
                                            taskTokensToAdd.add(taskToken);
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Consume:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove any instances of the task's token from incoming places
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i) == taskToken) {
                                                    tokenRemove[i] = true;
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n arbitrary instances of the task's token from incoming places
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i) == taskToken) {
                                                    tokenRemove[i] = true;
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            case Take:
                                switch (outReq.getMatchQuantity()) {
                                    case All:
                                        // Remove any instances of the task's token from incoming places and add them all to the outgoing place (may have duplicates)
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (inPlaceTokens.get(i) == taskToken) {
                                                    tokenRemove[i] = true;
                                                    taskTokensToAdd.add(taskToken);
                                                }
                                            }
                                        }
                                        break;
                                    case Number:
                                        // Remove n arbitrary instances of the task's token from incoming places and add them to the outgoing place
                                        int count = 0;
                                        boolean done = false;
                                        for (Place inPlace : inPlaceToTokenRemove.keySet()) {
                                            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
                                            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                                            for (int i = 0; i < inPlaceTokens.size(); i++) {
                                                if (count == outReq.getQuantity()) {
                                                    done = true;
                                                    break;
                                                }
                                                if (inPlaceTokens.get(i) == taskToken) {
                                                    tokenRemove[i] = true;
                                                    taskTokensToAdd.add(taskToken);
                                                    count++;
                                                }
                                            }
                                            if (done) {
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                        break;
                                }
                                break;
                            default:
                                LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                                break;
                        }
                        break;
                    default:
                        LOGGER.severe("\t\t\tEdge has unexpected token requirement: " + outReq);
                        break;
                }
            }
        }

        ////
        // Prepare to remove things
        ////
        Hashtable<Place, ArrayList<Token>> inPlaceToRemove = new Hashtable<Place, ArrayList<Token>>();
        for (Place inPlace : transition.getInPlaces()) {
            boolean[] tokenRemove = inPlaceToTokenRemove.get(inPlace);
            ArrayList<Token> inPlaceTokens = inPlace.getTokens();
            ArrayList<Token> tokensToRemove = new ArrayList<Token>();
            for (int i = 0; i < tokenRemove.length; i++) {
                if (tokenRemove[i]) {
                    tokensToRemove.add(inPlaceTokens.get(i));
                }
            }
            inPlaceToRemove.put(inPlace, tokensToRemove);
        }

        ////
        // Prepare to add things
        ////
        Hashtable<Place, ArrayList<Token>> outPlaceToAdd = new Hashtable<Place, ArrayList<Token>>();

        for (Place outPlace : transition.getOutPlaces()) {
            Hashtable<Place, boolean[]> inPlaceToTokenAdd = outPlaceToInPlaceToTokenAdd.get(outPlace);
            ArrayList<Token> taskTokensToAdd = outPlaceToTaskTokensToAdd.get(outPlace);
            Integer genericCount = outPlaceToGenericTokenAdd.get(outPlace);
            Hashtable<InputEvent, boolean[]> ieToRelTokenAdd = outPlaceToIeToRelTokenAdd.get(outPlace);

            ArrayList<Token> tokensToAdd = new ArrayList<Token>();
            for (Place inPlace : inPlaceToTokenAdd.keySet()) {
                ArrayList<Token> inPlaceTokens = inPlace.getTokens();
                boolean[] tokenAdd = inPlaceToTokenAdd.get(inPlace);
                for (int i = 0; i < tokenAdd.length; i++) {
                    if (tokenAdd[i]) {
                        tokensToAdd.add(inPlaceTokens.get(i));
                    }
                }
            }
            tokensToAdd.addAll(taskTokensToAdd);
            for (int i = 0; i < genericCount; i++) {
                tokensToAdd.add(Engine.getInstance().getGenericToken());
            }
            for (InputEvent ie : ieToRelTokenAdd.keySet()) {
                ArrayList<ProxyInt> relProxyList = ie.getRelevantProxyList();
                boolean[] tokenAdd = ieToRelTokenAdd.get(ie);
                for (int i = 0; i < tokenAdd.length; i++) {
                    if (tokenAdd[i]) {
                        Token proxyToken = Engine.getInstance().getToken(relProxyList.get(i));
                        if (proxyToken != null) {
                            tokensToAdd.add(proxyToken);
                        } else {
                            LOGGER.severe("Could not find proxy token for proxy " + relProxyList.get(i));
                        }
                    }
                }
            }

            outPlaceToAdd.put(outPlace, tokensToAdd);
        }

        // Actually remove things
        for (Place inPlace : transition.getInPlaces()) {
            leavePlace(inPlace, inPlaceToRemove.get(inPlace));
        }

        // Actually add things
        for (Place outPlace : transition.getOutPlaces()) {
            enterPlace(outPlace, outPlaceToAdd.get(outPlace), false);

        }

        // If we had a null RP blocking event we cloned for RPs, we should remove the clones
        //  we don't want them to still exist if we re-enter this transition
        // Get a list of blocking IE classes which we cloned
        ArrayList<Class> eventClassesToRemove = new ArrayList<Class>();
        for (InputEvent ie : transition.getInputEvents()) {
            if (ie.getBlocking() && ie.getRelevantProxyList() == null) {
                eventClassesToRemove.add(ie.getClass());

                // Clear lookup
                if (clonedIeTable.containsKey(ie)) {
                    HashMap<ProxyInt, InputEvent> proxyLookup = clonedIeTable.get(ie);
                    proxyLookup.clear();
                }

            }
        }
        // Get the clones of those classes
        ArrayList<InputEvent> clonedIesToRemove = new ArrayList<InputEvent>();
        for (InputEvent ie : transition.getInputEvents()) {
            if (eventClassesToRemove.contains(ie.getClass()) && ie.getRelevantProxyList() != null) {
                clonedIesToRemove.add(ie);
            }
        }
        // Remove the clones
        for (InputEvent clonedIe : clonedIesToRemove) {
            activeInputEvents.remove(clonedIe);
            transition.removeInputEvent(clonedIe);
            inputEventToTransitionMap.remove(clonedIe);
        }

        // Clear our input event status, in case we re-enter a previous place
        transition.clearInputEventStatus();

        ////
        // Now we can check the transitions we added tokens to
        ////
        LOGGER.log(EXE_T_LVL, "\tDone executing " + transition + ", checking for new transitions");
        for (Place outPlace : transition.getOutPlaces()) {
            for (Transition t2 : outPlace.getOutTransitions()) {
                boolean execute2 = checkTransition(t2);
                if (execute2) {
                    executeTransition(t2);
                }
            }
        }
    }

    private synchronized void leavePlace(Place place, ArrayList<Token> tokens) {
        LOGGER.info("Leaving " + place + " with " + place.getTokens() + " and taking " + tokens);

        // Remove Edge specified Tokens from Place
        for (Token token : tokens) {
            if (place.removeToken(token)) {
                LOGGER.log(Level.FINE, "\tRemoved " + token + " from " + place);
            } else {
                LOGGER.log(Level.SEVERE, "\tTrying to leave " + place + ", but am missing " + token);
                System.exit(0);
            }
        }

        // Check to see if any tokens are left in this place, or if it has become inactive
        if (place.getTokens().isEmpty()) {
            place.setIsActive(false);
            // This place is no longer active, unregister all input events from the event mapper
            for (Transition transition : place.getOutTransitions()) {
                boolean stillActive = false;
                for (Place inPlace : transition.getInPlaces()) {
                    if (inPlace != place && inPlace.getIsActive()) {
                        stillActive = true;
                        break;
                    }
                }
                if (!stillActive) {
                    for (InputEvent inputEvent : transition.getInputEvents()) {
                        if (!activeInputEvents.contains(inputEvent)) {
                            LOGGER.warning("Tried to remove IE: " + inputEvent + " from activeInputEvents, but it is not a member");
                        }
                        activeInputEvents.remove(inputEvent);

                        InputEventMapper.getInstance().unregisterEvent(inputEvent);

                        if (inputEventToTransitionMap.containsKey(inputEvent)) {
                            Transition t = inputEventToTransitionMap.remove(inputEvent);
                            LOGGER.log(Level.FINE, "\t Removed <" + inputEvent + ", " + t + "> from inputEventToTransitionMap");
                        } else {
                            LOGGER.warning("\t Tried to remove IE: " + inputEvent + " from inputEventToTransitionMap, but it is not a key");
                        }
                    }
                } else {
                    LOGGER.log(Level.FINE, "\t Not unregistering transition: " + transition + " as other connected Places are still active");
                }
            }

            // Tell watchers thet we have updates
            Engine.getInstance().leavePlace(this, place);
        }
    }

    public void applyTaskMapping(HashMap<TaskSpecification, TaskSpecification> taskMap, PlanManager parentPM) {
        LOGGER.log(Level.FINE, "\tApplying task map: " + taskMap.toString());
        for (TaskSpecification childTaskSpec : taskMap.keySet()) {
            TaskSpecification parentTaskSpec = taskMap.get(childTaskSpec);
            Token parentToken = parentPM.getToken(parentTaskSpec);
            Token childToken = getToken(childTaskSpec);
            LOGGER.log(Level.FINE, "\t\tSetting proxy: " + parentToken.getProxy() + " from parent token: " + parentToken + " to child token: " + childToken);
            childToken.setProxy(parentToken.getProxy());
        }
    }

    private synchronized void enterPlace(Place place, ArrayList<Token> tokens, boolean checkForTransition) {
        LOGGER.info("Entering " + place + " with Tokens: " + tokens + " with checkForTransition: " + checkForTransition + ", getInTransitions: " + place.getInTransitions() + ", inEdges: " + place.getInEdges() + ", getOutTransitions: " + place.getOutTransitions() + ", outEdges: " + place.getOutEdges());

        // 1 - Make note that this place should finish being entered before any of its
        //  transitions are actually executed
        synchronized (placesBeingEntered) {
            placesBeingEntered.add(place);
        }

        // 2 - Add the new tokens to the place
        for (Token token : tokens) {
            place.addToken(token);
        }

        // 3 - If this is our first time to enter the place, set up the requirements for all possible transitions attached to this place
        synchronized (activeInputEvents) {
            if (!place.getIsActive()) {
                // Things are not registered, do it now
                for (Transition transition : place.getOutTransitions()) {
                    for (InputEvent inputEvent : transition.getInputEvents()) {
                        if (!inputEventToTransitionMap.containsKey(inputEvent)) {
                            activeInputEvents.add(inputEvent);
                            InputEventMapper.getInstance().registerEvent(inputEvent, this);
                            LOGGER.log(Level.FINE, "\tAdding <" + inputEvent + "," + transition + "> to inputEventToTransitionMap");
                            inputEventToTransitionMap.put(inputEvent, transition);
                        }
                    }
                }
                place.setIsActive(true);
            }
        }

        // 4 - Invoke each OutputEvent with the new tokens
        processOutputEvents(place.getOutputEvents(), tokens);

        // 5 - Check for sub-missions and start them if required
        if (place.getSubMissions() != null && !place.getSubMissions().isEmpty()) {
            for (MissionPlanSpecification subMSpec : place.getSubMissions()) {
                LOGGER.info("\tStarting submission " + subMSpec);
                ArrayList<Token> subMissionTokens = new ArrayList<Token>();
                for (Token token : tokens) {
                    if (token.getType() == TokenType.Task && token.getProxy() != null) {
                        subMissionTokens.add(Engine.getInstance().getToken(token.getProxy()));
                    } else {
                        subMissionTokens.add(token);
                    }
                }
                PlanManager subMPlanManager = Engine.getInstance().spawnSubMission(subMSpec, subMissionTokens);
                // Add in existing variable definitions
                subMPlanManager.addParentVariables(variableNameToValue);
                planManagerToPlace.put(subMPlanManager, place);
                if (placeToActivePlanManagers.containsKey(place)) {
                    placeToActivePlanManagers.get(place).add(subMPlanManager);
                } else {
                    ArrayList<PlanManager> planManagers = new ArrayList<PlanManager>();
                    planManagers.add(subMPlanManager);
                    placeToActivePlanManagers.put(place, planManagers);
                }
                // Apply task mapping
                if (place.getSubMissionToTaskMap().containsKey(subMSpec)) {
                    HashMap<TaskSpecification, TaskSpecification> taskMap = place.getSubMissionToTaskMap().get(subMSpec);
                    subMPlanManager.applyTaskMapping(taskMap, this);
                }

                Engine.getInstance().addListener(this);
            }
        }

        // 6 - Tell listeners we have entered a place
        Engine.getInstance().enterPlace(this, place);

        // 7 - It is now safe to execute transitions leading out of this place
        synchronized (placesBeingEntered) {
            placesBeingEntered.remove(place);
        }

        // 8 - Check if any transitions out of this place should execute
        if (checkForTransition) {
            for (Transition transition : place.getOutTransitions()) {
                boolean execute = checkTransition(transition);
                if (execute) {
                    executeTransition(transition);
                };
            }
        }

        // 9 - Check if we are at an end place
        if (place.isEnd()) {
            LOGGER.log(Level.FINE, "\tReached an end place: " + place);
            boolean end = true;
            for (Vertex v : mSpec.getGraph().getVertices()) {
                if (v instanceof Place) {
                    Place checkPlace = (Place) v;
                    if ((checkPlace.getFunctionMode() == FunctionMode.HiddenRecovery || checkPlace.getFunctionMode() == FunctionMode.Recovery)
                            && checkPlace.getTokens().size() > 0
                            && !checkPlace.isEnd()) {
                        LOGGER.info("\t\tRecovery place has tokens in it! " + checkPlace);
                        end = false;
                    }
                }
            }
            if (end) {
                finishMission(place);
            }
        }
    }

    private void processOutputEvents(ArrayList<OutputEvent> outputEvents, ArrayList<Token> tokens) {
        for (OutputEvent oe : outputEvents) {
            LOGGER.log(Level.FINE, "Processing event " + oe + " with input variables " + oe.getVariables());
            // Write in values for any fields that were filled with a variable name
            if (oe.getVariables() != null) {
                for (String variableName : oe.getVariables().keySet()) {
                    Object variableValue = variableNameToValue.get(variableName);
                    LOGGER.log(Level.FINE, "\tLooking for " + variableName + " to set " + oe.getVariables().get(variableName) + " find " + variableValue + " " + variableNameToValue);
                    if (variableValue != null) {
                        Field f = oe.getVariables().get(variableName);
                        try {
                            LOGGER.log(Level.FINE, "\t\tAttempting to set " + f + " on " + oe + " to " + variableValue);
                            setField(f, oe, variableValue);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(PlanManager.class.getName()).log(Level.SEVERE, "Failed to set variable value on OutputEvent: " + ex, ex);
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "No value for variable: " + variableName);
                    }

                }
            } else {
                LOGGER.log(Level.FINE, "\tNo variables for " + oe);
            }
            // Find and invoke an appropriate handler
            EventHandlerInt eh = Engine.getInstance().getHandler(oe.getClass());
            if (eh != null) {
                LOGGER.log(Level.FINE, "Invoking handler: " + eh + " for OE: " + oe);
                eh.invoke(oe, tokens);
            } else {
                LOGGER.log(Level.SEVERE, "No handler for event of type " + oe.getClass());
            }

            // If we are aborting the mission, need to do some deregistration
            if (oe instanceof AbortMission) {
                abortMission();
            }
        }
    }

    @Override
    public void eventGenerated(InputEvent generatedEvent) {
        generatorEventQueue.offer(generatedEvent);
    }

    public void finishMission(Place endPlace) {
        LOGGER.info("Finishing plan at end place [" + endPlace + "]");
        Engine.getInstance().done(this);
        // Unregister any IEs still active
        for (InputEvent ie : activeInputEvents) {
            LOGGER.log(Level.FINE, "\tUnregistering active input event:" + ie);
            InputEventMapper.getInstance().unregisterEvent(ie);
        }
        activeInputEvents.clear();
    }

    public void abortMission() {
        // Get removed from Engine's plan manager listener list
        Engine.getInstance().abort(this);
        // Unregister any IEs still active
        for (InputEvent ie : activeInputEvents) {
            InputEventMapper.getInstance().unregisterEvent(ie);
        }
        activeInputEvents.clear();

        // Put token in end place?
    }

    public void processGeneratedEvent(InputEvent generatedEvent) {
        LOGGER.log(Level.FINE, "Processing generated event " + generatedEvent + " with event UUID " + generatedEvent.getId() + " mission UUID " + generatedEvent.getMissionId() + " and relevant proxy " + generatedEvent.getRelevantProxyList());
        if (generatedEvent.getMissionId() == null) {
            LOGGER.log(Level.FINE, "\tGenerated event has no mission UUID");
        } else {
            if (generatedEvent.getMissionId() != missionId) {
                LOGGER.log(Level.FINE, "\tMatching failed on mission UUID comparison");
                return;
            } else {
                LOGGER.log(Level.FINE, "\tMatching success on mission UUID comparison");
            }
        }
        // Events to add: A list of cloned IEs used to keep track of which proxy's have generated their instance of a Blocking IE
        HashMap<InputEvent, Transition> clonedEventsToAdd = new HashMap<InputEvent, Transition>();
        ArrayList<InputEvent> eventsToRemove = new ArrayList<InputEvent>();
        // One of our active transitions has an input event that resulted in a subscription to an InformationServiceProvider of this class
        boolean match;
        ArrayList<InputEvent> matchingEvents = new ArrayList<InputEvent>();

        // 1 - Go through our list of parameter events waiting to be fulfilled by a generated event
        for (InputEvent paramEvent : activeInputEvents) {
            LOGGER.log(Level.FINE, "\tChecking for match between parameter " + paramEvent + " to generated " + generatedEvent);

            // 2 - Compare variables used by the generated event to find the param event(s) it fulfills
            // 2a - Same class? 
            //  @todo - can remove this step with some additional data structures...
            if (paramEvent.getClass() != generatedEvent.getClass()) {
                LOGGER.log(Level.FINE, "\t\tMatching failed on class comparison: " + paramEvent.getClass() + " != " + generatedEvent.getClass());
                continue;
            } else {
                LOGGER.log(Level.FINE, "\t\tMatching success on class: " + paramEvent.getClass());
            }
            // 2b - If defined, we know the output event that caused this to occur
            //  Share a common OutputEvent uuid in a preceding place?
            if (generatedEvent.getRelevantOutputEventId() != null) {
                Transition transition = inputEventToTransitionMap.get(paramEvent);
                ArrayList<Place> inPlaces = transition.getInPlaces();
                match = false;
                for (Place inPlace : inPlaces) {
                    for (OutputEvent oe : inPlace.getOutputEvents()) {
                        if (oe.getId().equals(generatedEvent.getRelevantOutputEventId())) {
                            LOGGER.log(Level.FINE, "\t\tMatching success on relevant OE UUID: " + oe.getId());
                            match = true;
                        }
                    }
                }
                if (!match) {
                    LOGGER.log(Level.FINE, "\t\tMatching failed on UUID relevant OE UUID comparison - does not share a common OutputEvent uuid from a preceding place?");
                    continue;
                }
            } else {
                LOGGER.log(Level.FINE, "\t\tMatching success on UUID - no UUID to match against");
            }
            // 2c - If defined, this was some sort of proxy triggered event
            //  Have a token (Proxy or Task) for the proxy?
            //  Record the param event that matches this generator event
            if (generatedEvent.getRelevantProxyList() != null) {
                if (paramEvent.getRelevantProxyList() == null) {
                    // This is a proxy type event where no relevant proxies were specified in the OE, but relevant proxies exist in the resulting IE
                    if (paramEvent.getBlocking()) {
                        // For blocking input events, we require that all proxies on incoming edges be accounted for 
                        //  (ie, be contained in the RP list of the IE or a copy of it)
                        // For instance, a ProxyExploreArea would compute paths for a set of proxies to take, but each proxy will
                        //  individually send ProxyPathCompleted IEs to the system
                        // When an gen Blocking IE is received AND it matches the class of a param IE, AND the gen IE has RP, 
                        //  AND the param IE has null RP, check to see if we have created a copy of the param IE with the RP set to this proxy
                        //  If we have, we will/have match it, and if not, we should create it and set it to fulfilled. Repeat the IE copy 
                        //  process for each proxy token in all the transition's incoming places
                        //@todo Should it only be for incoming places with RP on the edge going to the transition?
                        LOGGER.log(Level.FINE, "\t\tHandling occurence of BlockingInputEvent with null RP in param: " + paramEvent + " and defined RP in gen: " + generatedEvent);

                        Transition transition = inputEventToTransitionMap.get(paramEvent);
                        HashMap<ProxyInt, InputEvent> proxyLookup;
                        if (clonedIeTable.containsKey(paramEvent)) {
                            proxyLookup = clonedIeTable.get(paramEvent);
                        } else {
                            proxyLookup = new HashMap<ProxyInt, InputEvent>();
                            clonedIeTable.put(paramEvent, proxyLookup);
                        }
                        ArrayList<ProxyInt> proxiesToCheck = new ArrayList<ProxyInt>();
                        // Get list of proxies that we will check that a cloned IE exists for 
                        // First add each proxy in incoming place with RP on the edge
                        for (InEdge inEdge : transition.getInEdges()) {
                            boolean hasRpReq = false;
                            for (TokenRequirement tokenReq : inEdge.getTokenRequirements()) {
                                if (tokenReq.getMatchCriteria() == TokenRequirement.MatchCriteria.RelevantToken) {
                                    hasRpReq = true;
                                    break;
                                }
                            }
                            if (hasRpReq) {
                                if (!(inEdge.getStart() instanceof Place)) {
                                    LOGGER.severe("Incoming edge to a transition was not a place!");
                                    System.exit(0);
                                }
                                for (Token token : ((Place) inEdge.getStart()).getTokens()) {
                                    if (token.getProxy() != null && !proxiesToCheck.contains(token.getProxy())) {
                                        proxiesToCheck.add(token.getProxy());
                                    }
                                }
                            }
                        }
                        // Next, look for incoming places with a Task token on the edge
                        //  If the Task token has an assigned proxy, add it to the list of proxies to check
                        for (InEdge inEdge : transition.getInEdges()) {
                            for (InTokenRequirement inReq : inEdge.getTokenRequirements()) {
                                if (inReq.getMatchCriteria() == TokenRequirement.MatchCriteria.SpecificTask) {
                                    Task task = taskNameToTask.get(inReq.getTaskName());
                                    if (task == null) {
                                        LOGGER.severe("Failed to find task with name " + inReq.getTaskName());
                                    }
                                    Token taskToken = getToken(task);
                                    if (taskToken == null) {
                                        LOGGER.severe("Failed to find token for task " + task);
                                    }
                                    if (taskToken.getProxy() != null && !proxiesToCheck.contains(taskToken.getProxy())) {
                                        proxiesToCheck.add(taskToken.getProxy());
                                    }
                                }
                            }
                        }
                        // Check that we have proxies to check, otherwise something is probably wrong
                        if (proxiesToCheck.isEmpty()) {
                            LOGGER.severe("Generated event RP != null and parameter event RP == null, but have no incoming edges with RP requirement!");
                        }

                        // Check that we have a cloned IE for each of the proxies in proxiesToCheck
                        //  If we don't, we need to create one
                        ArrayList<InputEvent> createdClones = new ArrayList<InputEvent>();
                        for (ProxyInt proxy : proxiesToCheck) {
                            if (!proxyLookup.containsKey(proxy)) {
                                // Clone the input event and then set the relevant proxy
                                InputEvent clonedEvent = paramEvent.copyForProxyTrigger();
                                ArrayList<ProxyInt> relevantProxyList = new ArrayList<ProxyInt>();
                                relevantProxyList.add(proxy);
                                clonedEvent.setRelevantProxyList(relevantProxyList);
                                // Add the cloned ie to the lookup table
                                proxyLookup.put(proxy, clonedEvent);
                                // Add the cloned ie to the list of input events waiting to be fulfilled
                                clonedEventsToAdd.put(clonedEvent, transition);
                                createdClones.add(clonedEvent);
                            }
                        }
                        // Mark the status of the null RP param event as "complete" so it won't prevent the transition from firing
                        //  It is still an active input event though, so events will continue to try and match against it,
                        //  creating more copies of it with new RP as needed
                        transition.setInputEventStatus(paramEvent, true);

                        // Now go through events we just created and ones matching proxies in the gen IE's RP
                        match = false;

                        for (ProxyInt proxy : generatedEvent.getRelevantProxyList()) {
                            if (proxyLookup.containsKey(proxy)) {
                                InputEvent matchingClonedEvent = proxyLookup.get(proxy);
                                if (!transition.getInputEventStatus(matchingClonedEvent)
                                        && createdClones.contains(matchingClonedEvent)) {
                                    // Hasn't been matched yet and the cloned event for the proxy was just created
                                    //  If it was previously created (ie is in activeInputEvents instead of eventsToAdd),
                                    //  it will be checked and matched - we don't want to add it to matchingEvents a second time here
                                    matchingEvents.add(matchingClonedEvent);
                                    match = true;
                                    LOGGER.log(Level.FINE, "\t\t\tMatching success on relevant proxy: " + proxy);
                                } else if (transition.getInputEventStatus(matchingClonedEvent)) {
                                    match = true;
                                    LOGGER.log(Level.WARNING, "\t\t\tMatching success on relevant proxy: " + proxy + ", but the corresponding cloned IE was already marked as having occurred!");
                                } else if (!createdClones.contains(matchingClonedEvent)) {
                                    match = true;
                                    LOGGER.log(Level.FINE, "\t\t\tMatching success on relevant proxy: " + proxy + ", but the corresponding cloned IE was already created!");
                                }
                            }
                        }
                        if (!match) {
                            LOGGER.log(Level.FINE, "\t\tMatching failed on relevant proxy - param event had no relevant proxy and no preceding place with a token containing the gen event's relevant proxy");
                            continue;
                        }
                    } else {
                        // For non-blocking input events, we don't require that all proxies on incoming edges are accounted for,
                        //  but for each proxy in the relevant proxy list we must have a token containing that proxy in an incoming Place
                        // For instance, an OE event requesting a selection of proxies would have no relevant proxies. The operator
                        //  would choose from the proxies contained in the tokens passed into the Transition.
                        //  The resulting IE would have contain the selected proxies in its relevant proxy list, which would
                        //  be a subset of the proxies contained in the tokens in the incoming places
                        LOGGER.log(Level.FINE, "\t\tHandling non-BlockingInputEvent " + paramEvent);
                        matchingEvents.add(paramEvent);
                    }
                } else {
                    // Check that the param event's proxy list matches the generated event's proxy list
                    match = true;
                    for (ProxyInt proxy : paramEvent.getRelevantProxyList()) {
                        if (!generatedEvent.getRelevantProxyList().contains(proxy)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        LOGGER.log(Level.FINE, "\t\tMatching success on relevant proxy - param event's relevant proxy matched gen event's relevant proxy");
                        // The process above has occurred previously, so we have versions of the ie with the proxy specified
                        matchingEvents.add(paramEvent);
                    } else {
                        LOGGER.log(Level.FINE, "\t\tMatching failed on relevant proxy - param event's relevant proxy did not match gen event's relevant proxy");
                        continue;
                    }
                }
            } else {
                LOGGER.log(Level.FINE, "\t\tMatching success on relevant proxy - gen event had no relevant proxy to match");
                // Generator event had no relevant proxy so no matching is necessary
                matchingEvents.add(paramEvent);
            }
        }

        LOGGER.log(Level.FINE, "\tResult of comparisons: add " + clonedEventsToAdd.size() + ", remove " + eventsToRemove.size() + ", update " + matchingEvents.size());

        for (InputEvent ie : clonedEventsToAdd.keySet()) {
            LOGGER.log(Level.FINE, "\t\tAdding " + ie);
            activeInputEvents.add(ie);
            Transition t = clonedEventsToAdd.get(ie);
            t.addInputEvent(ie);
            inputEventToTransitionMap.put(ie, t);
        }
        for (InputEvent ie : eventsToRemove) {
            // THIS LIST IS NEVER MODIFIED
            LOGGER.log(Level.FINE, "\t\tRemoving " + ie);
            activeInputEvents.remove(ie);
            Transition t = inputEventToTransitionMap.get(ie);
            t.removeInputEvent(ie);
            inputEventToTransitionMap.remove(ie);
        }
        for (InputEvent ie : matchingEvents) {
            LOGGER.log(Level.FINE, "\t\tUpdating " + ie);
            ie.setGeneratorEvent(generatedEvent);
            // Handle updated event
            processUpdatedParamEvent(ie);
        }
        LOGGER.log(Level.FINE, "\tFinished handling generated event: " + generatedEvent);
    }

    public void processUpdatedParamEvent(InputEvent updatedParamEvent) {
        LOGGER.log(Level.FINE, "@STAT Processing updated param event " + updatedParamEvent + " with UUID " + updatedParamEvent.getId() + " and relevant proxy " + updatedParamEvent.getRelevantProxyList());

        InputEvent generatorEvent = updatedParamEvent.getGeneratorEvent();
        if (!inputEventToTransitionMap.containsKey(updatedParamEvent)) {
            LOGGER.severe("No mapping from updated param event " + updatedParamEvent + " to transition!");
            return;
        }
        Transition transition = inputEventToTransitionMap.get(updatedParamEvent);

        // 1 - Check if there is an attached allocation to assign
        if (generatorEvent.getAllocation() != null) {
            Map<ITask, AbstractAsset> allocation = generatorEvent.getAllocation().getAllocation();
            LOGGER.log(Level.FINE, "\tInputEvent " + updatedParamEvent + " tied to " + transition + " occurred with an attach allocation: " + generatorEvent.getAllocation().toString());
            for (ITask task : allocation.keySet()) {
                Token taskToken = getToken((Task) task);
                if (taskToken != null) {
                    LOGGER.log(Level.FINE, "\t\tFound token " + taskToken + " for task " + task);
                    AbstractAsset asset = allocation.get(task);
                    ProxyInt proxy = Engine.getInstance().getProxyServer().getProxy(asset);
                    if (proxy != null) {
                        LOGGER.log(Level.FINE, "\t\tFound proxy " + proxy + " for asset " + asset);
                        taskToken.setProxy(proxy);
                    } else {
                        LOGGER.log(Level.SEVERE, "\t\tCould not find proxy for asset " + asset);
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "\t\tCould not find token for task " + task);
                }
            }
        }

        // 2a - Assign any missing instance params that were missing and have now been received
        if (generatorEvent instanceof MissingParamsReceived) {
            MissingParamsReceived paramsReceived = (MissingParamsReceived) generatorEvent;
            LOGGER.log(Level.FINE, "Writing parameters from MissingParamsReceived: " + paramsReceived);
            Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues = paramsReceived.getEventSpecToFieldValues();
            for (ReflectedEventSpecification eventSpec : eventSpecToFieldValues.keySet()) {
                Hashtable<Field, Object> fieldToValues = eventSpecToFieldValues.get(eventSpec);
                for (Field field : fieldToValues.keySet()) {
                    eventSpec.addFieldValue(field.getName(), fieldToValues.get(field));
                }
            }

            // Check if we have any required params that are still not defined
            ArrayList<ReflectedEventSpecification> missing = mSpec.getEventSpecsRequiringParams();
            if (missing.size() > 0) {
                for (ReflectedEventSpecification eventSpec : missing) {
                    LOGGER.warning("Event spec for " + eventSpec.getClassName() + " is missing fields");
                }
            }

            //@todo ugly code!
            //@todo what if we still have missing params that the operator decided not to fill out?
            // Should we instantiate the plan now?
            if (!mSpec.isInstantiated()) {
                mSpec.instantiate(missionId);
            }
        }

        // 2b - Assign any variable values returned in the generator event
        // The variables are on the InputEvent, because that has come from the spec, but the values are in the generator event
        HashMap<String, String> variables = updatedParamEvent.getWriteVariables();
        if (variables != null) {
            LOGGER.log(Level.FINE, "\tInputEvent " + updatedParamEvent + " tied to " + transition + " occurred with variables: " + variables);

            for (String fieldName : variables.keySet()) {
                LOGGER.log(Level.FINE, "\toccurred looking at variable " + fieldName);
                // For each variable in the response event
                Field definedField;
                try {
                    // Get the variable's Field object
                    definedField = ReflectionHelper.getField(generatorEvent.getClass(), fieldName);
                    if (definedField != null) {
                        definedField.setAccessible(true);
                        // Retrieve the value of the variable's Field object
                        variableNameToValue.put(variables.get(fieldName), definedField.get(generatorEvent));
                        // Add definition to sub-missions
                        for (PlanManager subMPM : planManagerToPlace.keySet()) {
                            subMPM.addParentVariable(variables.get(fieldName), definedField.get(generatorEvent));
                        }
                        LOGGER.log(Level.FINE, "\t\tVariable set " + variables.get(fieldName) + " = " + definedField.get(generatorEvent));
                    } else {
                        LOGGER.log(Level.WARNING, "\t\tGetting field failed: " + fieldName);
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.severe("\t\tFinding field for variable failed");
                } catch (IllegalAccessException ex) {
                    LOGGER.severe("\t\tFinding field for variable failed");
                } catch (SecurityException ex) {
                    LOGGER.severe("\t\tFinding field for variable failed");
                }
            }
        } else {
            LOGGER.log(Level.FINE, "\tInputEvent " + updatedParamEvent + " tied to " + transition + " occurred with no variables");
        }

        // 3 - Update the input event's status in the transition, remove it from the "active" input event list, and check if the transition should trigger now
        synchronized (activeInputEvents) {
            transition.setInputEventStatus(updatedParamEvent, true);
            boolean execute = checkTransition(transition);
            if (execute) {
                executeTransition(transition);
            }
        }
    }

    public void addParentVariables(HashMap<String, Object> parentVariableNameToValue) {
        variableNameToValue.putAll(parentVariableNameToValue);
    }

    public void addParentVariable(String name, Object value) {
        variableNameToValue.put(name, value);
        // Recursively add to sub missions
        for (PlanManager subMPM : planManagerToPlace.keySet()) {
            subMPM.addParentVariable(name, value);
        }
    }

    public ArrayList<ProxyInt> getRelevantProxies(Transition transition) {
        ArrayList<ProxyInt> list = new ArrayList<ProxyInt>();
        for (InputEvent ie : transition.getInputEvents()) {
            if (ie.getGeneratorEvent() == null) {
                LOGGER.log(Level.FINE, "\tie: " + ie + "\tgeneratorEvent is null: should be a null RP blocking IE: param IE RP = " + ie.getRelevantProxyList());
                continue;
            }
            if (ie.getGeneratorEvent().getRelevantProxyList() == null) {
                LOGGER.log(Level.FINE, "\tie: " + ie + "\trelevantProxyList is null");
                continue;
            }
            LOGGER.log(Level.FINE, "\tie: " + ie + "\trelevantProxyList: " + ie.getGeneratorEvent().getRelevantProxyList());
            list.addAll(ie.getGeneratorEvent().getRelevantProxyList());
        }
        return list;
    }

    public Place getStartPlace() {
        return startPlace;
    }

    /**
     * This has to recurse through the object, creating objects as it goes until
     * it finds the field that needs to be set
     *
     * @param f
     * @param e
     * @param v
     */
    private void setField(Field f, OutputEvent e, Object v) {
        LOGGER.log(Level.FINER, "Setting " + f + " on " + e + " to " + v);
        try {
            ArrayList options = new ArrayList();
            options.add(e);
            Object actualObject = null;
            while (options.size() > 0) {
                Object currObject = options.remove(0);
                try {
                    currObject.getClass().getDeclaredField(f.getName());
                    actualObject = currObject;
                } catch (Exception ex) {
                    for (Field field : currObject.getClass().getDeclaredFields()) {
                        try {
                            // If the field object needs to be created, do it, otherwise just add to options
                            Object o = field.get(currObject);
                            if (o == null) {
                                field.getType().newInstance();
                                field.set(currObject, o);
                            }
                            options.add(o);
                        } catch (Exception exception) {
                            LOGGER.log(Level.WARNING, "Failed to created object for " + field);
                        }
                    }
                }
            }
            if (actualObject != null) {
                f.set(actualObject, v);
                LOGGER.log(Level.FINER, "Variable set successfully: " + f + " on " + actualObject + " to " + v);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed: " + ex, ex);
        }
    }

    public void addDefaultStartToken(Token token) {
        startingTokens.add(token);
    }

    public void removeProxy(ProxyInt p) {
        // @todo Implement remove proxy
        LOGGER.log(Level.WARNING, "Removing proxy from plan unimplemented");
    }

    public Token getToken(Task task) {
        return taskToToken.get(task);
    }

    public Token getToken(TaskSpecification taskSpec) {
        return taskSpecToToken.get(taskSpec);
    }

    public Token createToken(TaskSpecification tokenSpec) {
        Token token = null;
        try {
            Object task = Class.forName(tokenSpec.getTaskClassName()).newInstance();
            ((Task) task).setName(tokenSpec.getName());
            token = new Token(tokenSpec.getName(), TokenType.Task, null, (Task) task);
            taskNameToTask.put(tokenSpec.getName(), (Task) task);
            taskToToken.put((Task) task, token);
            taskSpecToToken.put(tokenSpec, token);
            Logger.getLogger(this.getClass().getName()).log(Level.FINER, "\t\t\tCreated token " + token);
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (InstantiationException ie) {
            ie.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        return token;
    }

    public String getPlanName() {
        return planName;
    }

    @Override
    public void planCreated(PlanManager planManager, MissionPlanSpecification mSpec) {
    }

    @Override
    public void planStarted(PlanManager planManager) {
    }

    @Override
    public void planEnteredPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place place) {
    }

    @Override
    public void planFinished(PlanManager planManager) {
        Place place = planManagerToPlace.get(planManager);
        if (place != null) {
            ArrayList<PlanManager> activePMs = placeToActivePlanManagers.get(place);
            if (activePMs != null) {
                LOGGER.severe("Sub-mission mapping for place is null");
            } else if (activePMs.contains(planManager)) {
                activePMs.remove(planManager);
                if (activePMs.isEmpty()) {
                    // All sub-missions are now complete, check if we should execute transition
                    for (Transition transition : place.getOutTransitions()) {
                        boolean execute = checkTransition(transition);
                        if (execute) {
                            executeTransition(transition);
                        };
                    }
                }
            } else {
                LOGGER.severe("Sub-mission mapping for place did not contain plan manager");
            }
        } else {
            LOGGER.severe("Sub-mission plan manager has no mapped place");
        }
    }

    @Override
    public void planAborted(PlanManager planManager) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
