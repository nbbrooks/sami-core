package sami.engine;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import com.perc.mitpas.adi.mission.planning.task.Task;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.allocation.ResourceAllocation;
import sami.config.DomainConfigManager;
import sami.environment.EnvironmentListenerInt;
import sami.environment.EnvironmentProperties;
import sami.event.InputEvent;
import sami.event.TaskReassigned;
import sami.event.TaskReleased;
import sami.event.TaskUnassigned;
import sami.handler.EventHandlerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.ProjectSpecification;
import sami.mission.Token;
import sami.mission.Token.TokenType;
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
    private ProjectSpecification loadedProject = null;
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
    private final ArrayList<EnvironmentListenerInt> environmentListeners = new ArrayList<EnvironmentListenerInt>();
    private EnvironmentProperties environmentProperties = null;
    private ServiceServer serviceServer;
    private sami.uilanguage.UiClientInt uiClient = null;
    private sami.uilanguage.UiServerInt uiServer = null;
    // Configuration of output events and the handler classes that will execute them
    private final Hashtable<Class, EventHandlerInt> handlers = new Hashtable<Class, EventHandlerInt>();
    // Keeps track of variables coming in from InputEvents, to be used in OutputEvents
    private final HashMap<String, Object> variableNameToValue = new HashMap<String, Object>();
    private static final Object lock = new Object();

    private static class EngineHolder {

        public static final Engine INSTANCE = new Engine();
    }

    private Engine() {
        for (String className : DomainConfigManager.getInstance().domainConfiguration.serverList) {
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

        Hashtable<String, String> handlerMapping = DomainConfigManager.getInstance().domainConfiguration.eventHandlerMapping;
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

    public static Engine getInstance() {
        return EngineHolder.INSTANCE;
    }

    public void addListener(PlanManagerListenerInt planManagerListener) {
        synchronized (lock) {
            if (!planManagerListeners.contains(planManagerListener)) {
                planManagerListeners.add(planManagerListener);
            }
        }
    }

    public void addListener(TaskAllocationListenerInt taskAllocationListener) {
        synchronized (lock) {
            if (!taskAllocationListeners.contains(taskAllocationListener)) {
                taskAllocationListeners.add(taskAllocationListener);
            }
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

    public ProjectSpecification getProjectSpecification() {
        return loadedProject;
    }

    public void setProjectSpecification(ProjectSpecification loadedProject) {
        this.loadedProject = loadedProject;
    }

    private PlanManager spawnMission(MissionPlanSpecification mSpec, final ArrayList<Token> startingTokens) {
        // PlanManager will add mission's task tokens to the starting tokens
        UUID missionId = UUID.randomUUID();
        String planInstanceName = getUniquePlanName(mSpec.getName());
        MissionPlanSpecification mSpecInstance = mSpec.deepClone();
        final PlanManager pm = new PlanManager(mSpecInstance, missionId, planInstanceName, startingTokens);
        plans.add(pm);
        missionIdToPlanManager.put(missionId, pm);

        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planCreated(pm, mSpecInstance);
        }

        (new Thread() {
            public void run() {
                pm.start();
            }
        }).start();
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
        return spawnMission(mSpec, tokens);
    }

    public PlanManager spawnSubMission(MissionPlanSpecification mSpec, final ArrayList<Token> parentMissionTokens) {
        LOGGER.info("Spawning child mission from spec [" + mSpec + "] with parent tokens [" + parentMissionTokens + "]");
        return spawnMission(mSpec, parentMissionTokens);
    }

    public PlanManager getPlanManager(UUID missionId) {
        return missionIdToPlanManager.get(missionId);
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

    public void enterPlace(PlanManager planManager, Place p) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planEnteredPlace(planManager, p);
        }
    }

    public void leavePlace(PlanManager planManager, Place p) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planLeftPlace(planManager, p);
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

    public void done(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planFinished(planManager);
        }
        plans.remove(planManager);
    }

    public void abort(PlanManager planManager) {
        ArrayList<PlanManagerListenerInt> listenersCopy;
        synchronized (lock) {
            listenersCopy = (ArrayList<PlanManagerListenerInt>) planManagerListeners.clone();
        }
        for (PlanManagerListenerInt listener : listenersCopy) {
            listener.planAborted(planManager);
        }
        plans.remove(planManager);
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

    public void addEnvironmentLister(EnvironmentListenerInt listener) {
        environmentListeners.add(listener);
    }

    public EnvironmentProperties getEnvironmentProperties() {
        return environmentProperties;
    }

    public void setEnvironmentProperties(EnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
        for (EnvironmentListenerInt listener : environmentListeners) {
            listener.environmentUpdated();
        }
    }

    public Object getVariableValue(String variable) {
        return variableNameToValue.get(variable);
    }

    public void setVariableValue(String variable, Object value) {
        variableNameToValue.put(variable, value);
    }
}
