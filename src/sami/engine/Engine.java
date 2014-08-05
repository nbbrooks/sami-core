package sami.engine;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.config.DomainConfigManager;
import sami.environment.EnvironmentListenerInt;
import sami.environment.EnvironmentProperties;
import sami.handler.EventHandlerInt;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
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
    private ArrayList<PlanManager> plans = new ArrayList<PlanManager>();

    private ArrayList<PlanManagerListenerInt> planManagerListeners = new ArrayList<PlanManagerListenerInt>();
    private HashMap<UUID, PlanManager> missionIdToPlanManager = new HashMap<UUID, PlanManager>();
    // Token related items
    private final Token genericToken = new Token("G", TokenType.Generic, null, null);
    private ArrayList<Token> proxyTokens = new ArrayList<Token>();
    private HashMap<ProxyInt, Token> proxyToToken = new HashMap<ProxyInt, Token>();
    //
    private ArrayList<ProxyInt> proxies = new ArrayList<ProxyInt>();
    private ProxyServerInt proxyServer;
    private ArrayList<ObserverInt> observers = new ArrayList<ObserverInt>();
    private ObserverServerInt observerServer;
    private ArrayList<EnvironmentListenerInt> environmentListeners = new ArrayList<EnvironmentListenerInt>();
    private EnvironmentProperties environmentProperties = null;
    private ServiceServer serviceServer;
    private sami.uilanguage.UiClientInt uiClient = null;
    private sami.uilanguage.UiServerInt uiServer = null;
    // Configuration of output events and the handler classes that will execute them
    private Hashtable<Class, EventHandlerInt> handlers = new Hashtable<Class, EventHandlerInt>();
    // Keeps track of variables coming in from InputEvents, to be used in OutputEvents
    private final HashMap<String, Object> variableNameToValue = new HashMap<String, Object>();
    private static final Object lock = new Object();

    private PlanManager pm = null;

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

    public ProxyServerInt getProxyServer() {
        return proxyServer;
    }

    public ObserverServerInt getObserverServer() {
        return observerServer;
    }

    public PlanManager getPlanManager() {
        return pm;
    }

    public ArrayList<PlanManager> getPlans() {
        return plans;
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
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Spawning root mission from spec " + mSpec);
        return spawnMission(mSpec, tokens);
    }

    public PlanManager spawnSubMission(MissionPlanSpecification mSpec, final ArrayList<Token> parentMissionTokens) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Spawning child mission from spec " + mSpec);
        return spawnMission(mSpec, parentMissionTokens);
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
