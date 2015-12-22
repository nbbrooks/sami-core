package sami.engine;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import com.perc.mitpas.adi.mission.planning.task.Task;
import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import sami.allocation.ResourceAllocation;
import sami.config.DomainConfigManager;
import sami.event.AbortMissionReceived;
import sami.event.CompleteMissionReceived;
import sami.event.InputEvent;
import sami.event.TaskReassigned;
import sami.event.TaskReleased;
import sami.event.TaskUnassigned;
import sami.handler.EventHandlerInt;
import sami.logging.Recorder;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Token;
import sami.mission.Token.TokenType;
import sami.mission.Transition;
import sami.proxy.ProxyInt;
import sami.proxy.ProxyServerInt;
import sami.proxy.ProxyServerListenerInt;
import sami.sensor.ObserverInt;
import sami.sensor.ObserverServerInt;
import sami.sensor.ObserverServerListenerInt;
import sami.service.ServiceServer;
import sami.uilanguage.UiClientInt;
import sami.uilanguage.UiServerInt;

/**
 *
 * @todo The Engine is keeping track of PlanManagers
 *
 * @author pscerri
 */
public class Engine implements ProxyServerListenerInt, ObserverServerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(Engine.class.getName());
    // Plan related items
    private final ArrayList<PlanManager> plans = new ArrayList<PlanManager>();
    private final ArrayList<PlanManagerListenerInt> planManagerListeners = new ArrayList<PlanManagerListenerInt>();
    private final ArrayList<TaskAllocationListenerInt> taskAllocationListeners = new ArrayList<TaskAllocationListenerInt>();
    private final HashMap<UUID, PlanManager> missionIdToPlanManager = new HashMap<UUID, PlanManager>();
    private final HashMap<Task, PlanManager> taskToPlanManager = new HashMap<Task, PlanManager>();
    private ResourceAllocation resourceAllocation = new ResourceAllocation(new HashMap<ITask, AbstractAsset>(), null);
    // Token related items
    private final Token genericToken = new Token("G", TokenType.Generic, null, null);
    private final ArrayList<Token> proxyTokens = new ArrayList<Token>();
    private final HashMap<ProxyInt, Token> proxyToToken = new HashMap<ProxyInt, Token>();
    //
    private final ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();
    private ProxyServerInt proxyServer;
    private final ArrayList<ObserverInt> observers = new ArrayList<ObserverInt>();
    private ObserverServerInt observerServer;
    private ServiceServer serviceServer;
    private sami.uilanguage.UiClientInt uiClient = null;
    private sami.uilanguage.UiServerInt uiServer = null;
    // Configuration of output events and the handler classes that will execute them
    private final Hashtable<Class, EventHandlerInt> handlers = new Hashtable<Class, EventHandlerInt>();
    // Keeps track of variables coming in from InputEvents, to be used in OutputEvents
    private final HashMap<String, Object> globalVariableNameToValue = new HashMap<String, Object>();
    private final HashMap<PlanManager, HashMap<String, Object>> pmToVariableNameToValue = new HashMap<PlanManager, HashMap<String, Object>>();
    private final HashMap<PlanManager, ArrayList<PlanManager>> pmToSubPms = new HashMap<PlanManager, ArrayList<PlanManager>>();
    private final HashMap<PlanManager, PlanManager> subPmToParentPm = new HashMap<PlanManager, PlanManager>();
    private static final Object lock = new Object();
    // Variables for generating distinguishably unique color for each plan manager
    final Color[] colorDividers = new Color[]{Color.BLACK, Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.WHITE};
    Hashtable<PlanManager, Color[]> pmToColor = new Hashtable<PlanManager, Color[]>();
    double genInc = 2.0;
    double genFrac = colorDividers.length;
    ArrayList<Color> freedColors = new ArrayList<Color>(Arrays.asList(colorDividers));

    private static class EngineHolder {

        public static final Engine INSTANCE = new Engine();
    }

    public static Engine getInstance() {
        return EngineHolder.INSTANCE;
    }

    private Engine() {
        for (String className : DomainConfigManager.getInstance().getDomainConfiguration().serverList) {
            try {
                Class serverClass = Class.forName(className);
                Object serverElement = serverClass.getConstructor(new Class[]{}).newInstance();
                if (serverElement instanceof ProxyServerInt) {
                    proxyServer = (ProxyServerInt) serverElement;
                    proxyServer.addListener(this);
                }
                if (serverElement instanceof ObserverServerInt) {
                    observerServer = (ObserverServerInt) serverElement;
                    observerServer.addListener(this);
                }
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            } catch (InstantiationException ie) {
                ie.printStackTrace();
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            } catch (NoSuchMethodException nsme) {
                nsme.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
        }
        if (proxyServer == null) {
            LOGGER.log(Level.SEVERE, "Failed to find Proxy Server in domain configuration!");
        }
        if (observerServer == null) {
            LOGGER.log(Level.SEVERE, "Failed to find Observer Server in domain configuration!");
        }

        Hashtable<String, String> handlerMapping = DomainConfigManager.getInstance().getDomainConfiguration().eventHandlerMapping;
        Class eventClass, handlerClass;
        EventHandlerInt handlerObject;
        HashMap<String, EventHandlerInt> handlerObjects = new HashMap<String, EventHandlerInt>();
        String handlerClassName;
        for (String ieClassName : handlerMapping.keySet()) {
            handlerClassName = handlerMapping.get(ieClassName);
            try {
                eventClass = Class.forName(ieClassName);
                handlerClass = Class.forName(handlerClassName);
                if (!handlerObjects.containsKey(handlerClassName)) {
                    // First use of this handler class, create an instance and add it to our hashmap
                    EventHandlerInt newHandlerObject = (EventHandlerInt) handlerClass.newInstance();
                    if (ProxyServerListenerInt.class.isInstance(newHandlerObject)) {
                        proxyServer.addListener((ProxyServerListenerInt) newHandlerObject);
                    }
                    handlerObjects.put(handlerClassName, newHandlerObject);
                }
                handlerObject = handlerObjects.get(handlerClassName);
                handlers.put(eventClass, handlerObject);
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void addListener(PlanManagerListenerInt planManagerListener) {
        synchronized (lock) {
            if (!planManagerListeners.contains(planManagerListener)) {
                planManagerListeners.add(planManagerListener);
            }
        }
    }

    public void removeListener(PlanManagerListenerInt planManagerListener) {
        synchronized (lock) {
            planManagerListeners.remove(planManagerListener);
        }
    }

    public void addListener(TaskAllocationListenerInt taskAllocationListener) {
        synchronized (lock) {
            if (!taskAllocationListeners.contains(taskAllocationListener)) {
                taskAllocationListeners.add(taskAllocationListener);
            }
        }
    }

    public void removeListener(TaskAllocationListenerInt taskAllocationListener) {
        synchronized (lock) {
            taskAllocationListeners.remove(taskAllocationListener);
        }
    }

    public ResourceAllocation getTaskAllocation() {
        if (resourceAllocation == null) {
            return null;
        }
        return resourceAllocation.clone();
    }

    public ProxyServerInt getProxyServer() {
        return proxyServer;
    }

    public ObserverServerInt getObserverServer() {
        return observerServer;
    }

    public Color[] getPlanManagerColor(PlanManager pm) {
        if (pmToColor.containsKey(pm)) {
            return pmToColor.get(pm);
        }
        Color background;
        if (!freedColors.isEmpty()) {
            // Use a color from a previously completed/aborted PM
            background = freedColors.remove(0);
        } else {
            // Generate the next color in the sequence
            genFrac += genInc;
            if (genFrac > colorDividers.length - 1) {
                genInc /= 2;
                genFrac = genInc / 2;
            }
            background = getMixedColor(genFrac);
        }
        Color foreground = getForeground(background);
        Color[] doubleColor = new Color[]{background, foreground};
        pmToColor.put(pm, doubleColor);
        return doubleColor;
    }

    private Color getForeground(Color background) {
        if ((background.getRed() < 120 && background.getGreen() < 120 && background.getBlue() < 120)
                || background.getRed() + background.getGreen() * 2 + background.getBlue() < 382) {
            // Use white text for dark backgrounds
            //  Weight green a bit because it is so freaking bright
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }

    private void freePlanManagerColor(PlanManager pm) {
        Color[] doubleColor = pmToColor.remove(pm);
        if (doubleColor != null) {
            freedColors.add(doubleColor[0]);
        } else {
            LOGGER.warning("Tried to free color for PlanManager " + pm + ", but there was no match");
        }
    }

    private Color getMixedColor(double fraction) {
        if (fraction < 0 || fraction > colorDividers.length - 1) {
            LOGGER.warning("Request mixed color for fraction " + fraction + ", but color array is length " + colorDividers.length);
            return colorDividers[0];
        }
        if (fraction == colorDividers.length - 1) {
            return colorDividers[colorDividers.length - 1];
        }
        int trunc = (int) fraction;
        double dec1 = fraction % 1;
        double dec2 = 1 - dec1;
        Color c = new Color((int) (colorDividers[trunc].getRed() * dec1 + colorDividers[trunc + 1].getRed() * dec2) / 2,
                (int) (colorDividers[trunc].getGreen() * dec1 + colorDividers[trunc + 1].getGreen() * dec2) / 2,
                (int) (colorDividers[trunc].getBlue() * dec1 + colorDividers[trunc + 1].getBlue() * dec2) / 2);
        return c;
    }

    private PlanManager setUpPlanManager(MissionPlanSpecification mSpec, final ArrayList<Token> startingTokens, boolean isSharedSm) {
        // PlanManager will add mission's task tokens to the starting tokens
        UUID missionId = UUID.randomUUID();
        String planInstanceName = getUniquePlanName(mSpec.getName());
        MissionPlanSpecification mSpecInstance = mSpec.deepClone();
        final PlanManager pm = new PlanManager(mSpecInstance, missionId, planInstanceName, startingTokens, isSharedSm);
        plans.add(pm);
        // Create plan manager color
        getPlanManagerColor(pm);
        // Add lookups
        missionIdToPlanManager.put(missionId, pm);
        pmToVariableNameToValue.put(pm, new HashMap<String, Object>());
        // Now finish setup actions which require lookup tables
        pm.finishSetup();

        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planCreated(pm, mSpecInstance);
        }

        return pm;
    }

    public PlanManager spawnRootMission(MissionPlanSpecification mSpec) {
        ArrayList<Token> tokens = new ArrayList<Token>();
        // Add in generic token
        tokens.add(genericToken);
        // Add in already existing proxy tokens
        for (Token proxyToken : proxyTokens) {
            tokens.add(proxyToken);
        }
        LOGGER.info("Spawning root mission from spec [" + mSpec + "]");
        final PlanManager rootPm = setUpPlanManager(mSpec, tokens, false);
        if (!pmToSubPms.containsKey(rootPm)) {
            // Need this check because setUpPlanManager could create entry with a call to spawnSubMission for a shared SM
            pmToSubPms.put(rootPm, new ArrayList<PlanManager>());
        }
        Recorder.getInstance().spawnRootMission(mSpec, rootPm);

        (new Thread() {
            public void run() {
                rootPm.start();
            }
        }).start();

        return rootPm;
    }

    public PlanManager spawnSubMission(MissionPlanSpecification mSpec, PlanManager parentPm, final ArrayList<Token> parentMissionTokens, boolean isSharedSm) {
        LOGGER.info("Spawning child mission from spec [" + mSpec + "] with parent tokens [" + parentMissionTokens + "]");
        final PlanManager subPm = setUpPlanManager(mSpec, parentMissionTokens, isSharedSm);
        subPmToParentPm.put(subPm, parentPm);
        if (pmToSubPms.containsKey(parentPm)) {
            pmToSubPms.get(parentPm).add(subPm);
        } else {
            ArrayList<PlanManager> subPms = new ArrayList<PlanManager>();
            subPms.add(subPm);
            pmToSubPms.put(parentPm, subPms);
        }

        (new Thread() {
            public void run() {
                subPm.start();
            }
        }).start();

        return subPm;
    }

    public PlanManager getPlanManager(UUID missionId) {
        return missionIdToPlanManager.get(missionId);
    }

    public ArrayList<PlanManager> getSubPlanManagers(UUID parentMissionId) {
        PlanManager parentPm = missionIdToPlanManager.get(parentMissionId);
        if (pmToSubPms.containsKey(parentPm)) {
            return ((ArrayList< PlanManager>) pmToSubPms.get(parentPm).clone());
        } else {
            return new ArrayList<PlanManager>();
        }
    }

    public void linkTask(Task task, PlanManager planManager) {
        taskToPlanManager.put(task, planManager);
    }

    public void unlinkTask(Task task) {
        taskToPlanManager.remove(task);
    }

    public ResourceAllocation getAllocation() {
        return resourceAllocation;
    }

    public void applyAllocation(ResourceAllocation resourceAllocation) {
        LOGGER.fine("Applying allocation: " + resourceAllocation);
        // Create lists for TaskUnassigned, TaskReleased, and TaskReassigned events that should be generated
        ArrayList<InputEvent> taskEvents = new ArrayList<InputEvent>();
        // Make TaskUnassigned events for tasks that are unallocated
        for (ITask task : resourceAllocation.getUnallocatedTasks()) {
            if (taskToPlanManager.containsKey((Task) task)) {
                PlanManager pm = taskToPlanManager.get((Task) task);
                taskEvents.add(new TaskUnassigned(pm.missionId, (Task) task));
            } else {
                LOGGER.severe("Could not find PM for task: " + task);
            }
        }
        // Make TaskReassigned events for tasks that have changed assets
        for (ITask task : resourceAllocation.getTaskToAsset().keySet()) {
            // Don't need to check for tasks that are now unassigned, have dedicated list for that
            Token taskToken = getToken((Task) task);
            if (taskToken.getProxy() != null && taskToken.getProxy() != proxyServer.getProxy((resourceAllocation.getTaskToAsset().get(task)))) {
                if (taskToPlanManager.containsKey((Task) task)) {
                    PlanManager pm = taskToPlanManager.get((Task) task);
                    taskEvents.add(new TaskReassigned(pm.missionId, (Task) task));
                } else {
                    LOGGER.severe("Could not find PM for task: " + task);
                }
            }
        }
        // Update task tokens' proxy values and send new allocations to proxies
        //  Get TaskDelayed and TaskReleased events from proxies
        for (AbstractAsset asset : resourceAllocation.getAssetToTasks().keySet()) {
            ProxyInt proxy = proxyServer.getProxy(asset);
            if (proxy == null) {
                LOGGER.severe("Could not find ProxyInt for AbstractAsset: " + asset);
                continue;
            }
            LOGGER.log(Level.FINE, "Found proxy " + proxy + " for asset " + asset);
            ArrayList<ITask> iTasks = resourceAllocation.getAssetToTasks().get(asset);
            ArrayList<Task> tasks = new ArrayList<Task>();
            for (ITask iTask : iTasks) {
                Token taskToken = getToken((Task) iTask);
                LOGGER.log(Level.FINE, "\tFound token " + taskToken + " for task " + iTask);
                taskToken.setProxy(proxy);
                tasks.add(taskToken.getTask());
            }
            ArrayList<InputEvent> proxyTaskEvents = proxy.setTasks(tasks);
            taskEvents.addAll(proxyTaskEvents);
        }
        // Save allocation
        this.resourceAllocation = resourceAllocation;
        // Finally, generate the TaskUnassigned, TaskReassigned, TaskDelayed, and TaskReleased events
        ArrayList<PlanManager> pmList = new ArrayList<PlanManager>();
        for (InputEvent ie : taskEvents) {
            pmList.clear();
            for (Task task : ie.getRelevantTaskList()) {
                if (taskToPlanManager.containsKey((Task) task)) {
                    PlanManager pm = taskToPlanManager.get(task);
                    if (!pmList.contains(pm)) {
                        if (ie instanceof TaskReleased) {
                            // Because we are no longer associate this task with this proxy, we need a different mechanism 
                            //  to allow plan developers to have the proxy formerly responsible to the task to respond to
                            //  the task release - do this by "silently" adding the proxy's proxy token to the place, 
                            //  which will allow the TaskReleased to trigger based on relevant proxy token
                            if (ie.getRelevantProxyList().size() != 1) {
                                LOGGER.warning("Expected TaskReleased to have 1 relevant proxy, has [" + ie.getRelevantProxyList() + "]");
                            }
                            for (ProxyInt proxy : ie.getRelevantProxyList()) {
                                pm.addProxyForTask(proxy, ((TaskReleased) ie).getTask());
                            }
                        }
                        LOGGER.fine("Telling pm [" + pm + "] about allocation change for task [" + task + "] with IE [" + ie + "]");
                        pmList.add(pm);
                        pm.eventGenerated(ie);
                    }
                } else {
                    LOGGER.severe("Could not find PM for task: " + task);
                }
            }
        }
        // Notify listeners
        for (TaskAllocationListenerInt listener : taskAllocationListeners) {
            listener.allocationApplied(resourceAllocation.clone());
        }
    }

    public void taskCompleted(ITask task) {
        // Notify listeners
        for (TaskAllocationListenerInt listener : taskAllocationListeners) {
            listener.taskCompleted(task);
        }
    }

    public PlanManager getPlanManager(Task task) {
        if (!taskToPlanManager.containsKey(task)) {
            LOGGER.severe("No mapping from task [" + task + "] to a plan manager, mapping is [" + taskToPlanManager.toString() + "]");
        }
        return taskToPlanManager.get(task);
    }

    public ServiceServer getServiceServer() {
        return serviceServer;
    }

    public void setServiceServer(ServiceServer serviceServer) {
        this.serviceServer = serviceServer;
        InputEventMapper.getInstance().setServiceServer(serviceServer);
    }

    public UiClientInt getUiClient() {
        return uiClient;
    }

    public void setUiClient(UiClientInt uiClient) {
        this.uiClient = uiClient;
    }

    public UiServerInt getUiServer() {
        return uiServer;
    }

    public void setUiServer(sami.uilanguage.UiServerInt uiServer) {
        this.uiServer = uiServer;
    }

    public void created(PlanManager planManager, MissionPlanSpecification spec) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planCreated(planManager, spec);
        }
    }

    public void started(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planStarted(planManager);
        }
    }

    public void instantiated(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planInstantiated(planManager);
        }
    }

    public void enteredPlace(PlanManager planManager, Place p) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planEnteredPlace(planManager, p);
        }
    }

    public void leftPlace(PlanManager planManager, Place p) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planLeftPlace(planManager, p);
        }
    }

    public void executedTransition(PlanManager planManager, Transition t) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planExecutedTransition(planManager, t);
        }
    }

    public void repaintPlan(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planRepaint(planManager);
        }
    }

    /**
     * The plan has been finished and should be cleaned up
     *
     * @param planManager
     */
    public void done(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planFinished(planManager);
        }
        plans.remove(planManager);
        freePlanManagerColor(planManager);

        //@todo add CompleteMission events so we can comment this out again
        // Do this in CoreEventHandler
        for (ProxyInt proxy : proxies) {
            proxy.completeMission(planManager.missionId);
        }

        // Now force completion of any sub-missions
        //  Only do immediate children, as this will recurse
        if (pmToSubPms.containsKey(planManager)) {
            for (PlanManager subPm : pmToSubPms.get(planManager)) {
                // Check that sub PM has an active CompleteMissionReceived that will handle this
                //  Shared SMs may not currently have any active places, so the IE will not trigger an transition
                //  Non-shared SMs should have an active place which will trigger a transition, but check just in case
                boolean found = false;
                ArrayList<InputEvent> smActiveInputEvents = subPm.getActiveInputEventsClone();
                for (InputEvent ie : smActiveInputEvents) {
                    if (ie instanceof CompleteMissionReceived) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // No active CompleteMissionReceived in the SM
                    if (!subPm.getIsSharedSm()) {
                        LOGGER.severe("Want to send CompleteMissionReceived to non-shared SM, but it has no active IE, manually ending SM");
                    }
                    // End this PM manually
                    done(subPm);
                } else {
                    subPm.eventGenerated(new CompleteMissionReceived(subPm.missionId));
                }
            }
        }
    }

    /**
     * The shared sub-plan has reached an "normal/return" place Tokens should be
     * passed up to the parent mission, but the plan should not be cleaned up
     *
     * @param planManager
     */
    public void sharedSmReachedEnd(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.sharedSubPlanAtReturn(planManager);
        }
    }

    /**
     * The plan has been aborted and should be cleaned up
     *
     * @param planManager
     */
    public void abort(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planAborted(planManager);
        }
        plans.remove(planManager);
        freePlanManagerColor(planManager);

        // Now abort any sub-missions
        //  Only do immediate children, as this will recurse
        if (pmToSubPms.containsKey(planManager)) {
            for (PlanManager subPm : pmToSubPms.get(planManager)) {
                // Check that sub PM has an active AbortMissionReceived that will handle this
                //  Shared SMs may not currently have any active places, so the IE will not trigger an transition
                //  Non-shared SMs should have an active place which will trigger a transition, but check just in case
                boolean found = false;
                ArrayList<InputEvent> smActiveInputEvents = subPm.getActiveInputEventsClone();
                for (InputEvent ie : smActiveInputEvents) {
                    if (ie instanceof AbortMissionReceived) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // No active AbortMissionReceived in the SM
                    if (!subPm.getIsSharedSm()) {
                        LOGGER.severe("Want to send AbortMissionReceived to non-shared SM, but it has no active IE, manually aborting SM");
                    }
                    // Abort this PM manually
                    //@todo this only goes one layer deep - what if SM has SM also without AbortMissionReceived?
                    abort(subPm);
                } else {
                    subPm.eventGenerated(new AbortMissionReceived(subPm.missionId));
                }
            }
        }
    }

    @Override
    public void proxyAdded(ProxyInt p) {
        proxies.add(p);
        Token proxyToken = createToken(p);
        for (PlanManager planManager : plans) {
            planManager.addDefaultStartToken(proxyToken);
        }

//        Location waypoint = new Location(40.44515205369163, -80.01877404355538, 0);
//        ArrayList<Location> waypoints = new ArrayList<Location>();
//        waypoints.add(waypoint);
//        Path path = new PathUtm(waypoints);
//        p.setPath(path);
    }

    @Override
    public void proxyRemoved(ProxyInt p) {
        proxies.remove(p);
    }

    @Override
    public void observerAdded(ObserverInt p) {
        observers.add(p);
    }

    @Override
    public void observerRemoved(ObserverInt p) {
        observers.remove(p);
    }

    public EventHandlerInt getHandler(Class outputEventClass) {
        return handlers.get(outputEventClass);
    }

    public Token getGenericToken() {
        return genericToken;
    }

    public Token getToken(ProxyInt proxy) {
        return proxyToToken.get(proxy);
    }

    public Token getToken(Task task) {
        if (taskToPlanManager.containsKey(task)) {
            return taskToPlanManager.get(task).getToken(task);
        }
        return null;
    }

    public ArrayList<ProxyInt> getAllProxies() {
        return (ArrayList<ProxyInt>) proxies.clone();
    }

    public Token createToken(ProxyInt proxy) {
        Token token = new Token(proxy.getProxyName(), TokenType.Proxy, proxy, null);
        proxyToToken.put(proxy, token);
        proxyTokens.add(token);
        return token;
    }

    public String getUniquePlanName(String planName) {
        boolean validName = false;
        while (!validName) {
            validName = true;
            for (PlanManager pm : plans) {
                if (pm.getPlanName().equals(planName)) {
                    int index = planName.length() - 1;
                    while (index >= 0 && (int) planName.charAt(index) >= (int) '0' && (int) planName.charAt(index) <= (int) '9') {
                        index--;
                    }
                    index++;
                    if (index == planName.length()) {
                        planName += '2';
                    } else {
                        int number = Integer.parseInt(planName.substring(index));
                        planName = planName.substring(0, index) + (number + 1);
                    }
                    validName = false;
                    break;
                }
            }
        }
        return planName;
    }

    public ArrayList<String> getVariablesInScope(PlanManager pmScope) {
        ArrayList<String> useableVariableNames = new ArrayList<String>();
        // Add variables in current PM, if one exists
        if (pmScope != null && pmToVariableNameToValue.get(pmScope) != null) {
            useableVariableNames.addAll(pmToVariableNameToValue.get(pmScope).keySet());
            // Add variables from parent PMs, if any exist
            PlanManager recursiveScope = pmScope;
            while (subPmToParentPm.containsKey(recursiveScope) && subPmToParentPm.get(recursiveScope) != null) {
                recursiveScope = subPmToParentPm.get(recursiveScope);
                if (pmToVariableNameToValue.get(recursiveScope) != null) {
                    for (String variableName : pmToVariableNameToValue.get(recursiveScope).keySet()) {
                        if (!useableVariableNames.contains(variableName)) {
                            useableVariableNames.add(variableName);
                        }
                    }
                }
            }
        }
        // Add all global variables
        for (String variableName : globalVariableNameToValue.keySet()) {
            if (!useableVariableNames.contains(variableName)) {
                useableVariableNames.add(variableName);
            }
        }

        return useableVariableNames;
    }

    public Object getVariableValue(String variable, PlanManager pmScope) {
        if (pmScope == null) {
            return globalVariableNameToValue.get(variable);
        } else {
            // Check for local definition, if none, progressively check parent missions (if pmScope is a sub-mission)
            ObjectWithDepth objectWithDepth = getParentVariableValue(variable, pmScope, 0);
            if (objectWithDepth.object != null) {
                return objectWithDepth.object;
            } else {
                // Not defined in the PM or a parent PM, check for global variable definition or else return null;
                return globalVariableNameToValue.get(variable);
            }
        }
    }

    private ObjectWithDepth getParentVariableValue(String variable, PlanManager pmScope, int depth) {
        if (variable == null || pmScope == null) {
            // Shouldn't happen
            return new ObjectWithDepth(null, Integer.MAX_VALUE);
        }
        if (pmToVariableNameToValue.get(pmScope).containsKey(variable)) {
            return new ObjectWithDepth(pmToVariableNameToValue.get(pmScope).get(variable), depth);
        } else if (subPmToParentPm.containsKey(pmScope)) {
            return getParentVariableValue(variable, subPmToParentPm.get(pmScope), depth + 1);
        } else {
            return new ObjectWithDepth(null, depth);
        }
    }

    private ObjectWithDepth getSubmissionVariableValue(String variable, PlanManager pmScope, int depth) {
        if (variable == null || pmScope == null) {
            // Shouldn't happen
            return new ObjectWithDepth(null, Integer.MAX_VALUE);
        }
        HashMap<String, Object> variableNameToValue = pmToVariableNameToValue.get(pmScope);

        if (variableNameToValue.containsKey(variable)) {
            return new ObjectWithDepth(variableNameToValue.get(variable), depth);
        } else {
            ArrayList<PlanManager> subPms = pmToSubPms.get(pmScope);
            int minDepth = Integer.MAX_VALUE;
            Object value = null;
            for (PlanManager subPm : subPms) {
                ObjectWithDepth objectWithDepth = getSubmissionVariableValue(variable, subPm, depth + 1);
                if (objectWithDepth.object != null && objectWithDepth.depth < minDepth) {
                    minDepth = objectWithDepth.depth;
                    value = objectWithDepth.object;
                }
            }
            return new ObjectWithDepth(value, minDepth);
        }
    }

    public void setVariableValue(String variable, Object value, PlanManager pmScope) {
        if (pmScope == null) {
            globalVariableNameToValue.put(variable, value);
        } else {
            pmToVariableNameToValue.get(pmScope).put(variable, value);
        }
    }

    public boolean redefineVariableValue(String variable, Object newValue, PlanManager currentPmScope) {
        if (currentPmScope == null) {
            // Global scope variable
            if (globalVariableNameToValue.containsKey(variable)) {
                // Check if new value is the same or superclass of current value
                Object currentValue = globalVariableNameToValue.get(variable);

                boolean match = objectClassMatch(newValue, currentValue);
                if (!match) {
                    LOGGER.warning("Redefining variable \"" + variable + "\" with current definition \"" + currentValue.toString() + "\" (" + currentValue.getClass().getSimpleName() + ") with value of different class \"" + newValue.toString() + "\" (" + newValue.getClass().getSimpleName() + ")");
                }
                // Write value, even if new value's class is different
                globalVariableNameToValue.put(variable, newValue);

                return true;
            } else {
                // Failed to match variable
                LOGGER.severe("Failed to find existing definition to redefine for variable \"" + variable + "\"");
                return false;
            }
        } else {
            if (pmToVariableNameToValue.get(currentPmScope).containsKey(variable)) {
                // Check if new value is the same or superclass of current value
                Object currentValue = pmToVariableNameToValue.get(currentPmScope).get(variable);

                boolean match = objectClassMatch(newValue, currentValue);
                if (!match) {
                    LOGGER.warning("Redefining variable \"" + variable + "\" with current definition \"" + currentValue.toString() + "\" (" + currentValue.getClass().getSimpleName() + ") with value of different class \"" + newValue.toString() + "\" (" + newValue.getClass().getSimpleName() + ")");
                }
                // Write value, even if new value's class is different
                pmToVariableNameToValue.get(currentPmScope).put(variable, newValue);

                return true;
            } else {
                // Recurse with parent PM or NULL PM to indicate checking global variables
                return redefineVariableValue(variable, newValue, subPmToParentPm.get(currentPmScope));
            }
        }
    }

    private boolean objectClassMatch(Object objectA, Object objectB) {
        boolean match = false;
        // For each field with a write variable assigned to it
        if (java.util.Hashtable.class.isAssignableFrom(objectA.getClass()) && java.util.Hashtable.class.isAssignableFrom(objectB.getClass())) {
            //@todo Check key/value compatibility
            match = true;
        } else if (java.util.List.class.isAssignableFrom(objectA.getClass()) && java.util.List.class.isAssignableFrom(objectB.getClass())) {
            //@todo Check item type compatibility
            match = true;
        } else if (objectA.getClass().isAssignableFrom(objectB.getClass())) {
            match = true;
        }
        return match;
    }

    public void clearGlobalVariables() {
        globalVariableNameToValue.clear();
    }

    public PlanManager getParentPm(PlanManager subPm) {
        return subPmToParentPm.get(subPm);
    }

    class ObjectWithDepth {

        public Object object;
        public int depth;

        public ObjectWithDepth(Object object, int depth) {
            this.object = object;
            this.depth = depth;
        }
    }

    // Color generation test
    public Color[] getUniqueColor() {
        Color[] doubleColor = new Color[2];
        if (!freedColors.isEmpty()) {
            doubleColor[0] = freedColors.remove(0);
        } else {
            genFrac += genInc;
            if (genFrac > colorDividers.length - 1) {
                genInc /= 2;
                genFrac = genInc / 2;
            }
            // Now generate the mixed color
            doubleColor[0] = getMixedColor(genFrac);
        }
        doubleColor[1] = getForeground(doubleColor[0]);
        return doubleColor;
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.getContentPane().setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));

        for (int i = 0; i < 50; i++) {
            JLabel l = new JLabel("Label " + i);
            Color[] dc = Engine.getInstance().getUniqueColor();
            l.setBackground(dc[0]);
            l.setForeground(dc[1]);
            l.setOpaque(true);
            f.add(l);
        }
        f.pack();
        f.setSize(new Dimension(400, 800));
        f.setVisible(true);
    }
}
