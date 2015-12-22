package sami.handler;

import com.perc.mitpas.adi.mission.planning.task.Task;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.event.AbortMission;
import sami.event.AbortMissionReceived;
import sami.event.CompleteMission;
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.GetAllProxyTokens;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import sami.event.ProxyAbortMissionReceived;
import sami.event.ProxyStartTimer;
import sami.event.ProxyTimerExpired;
import sami.event.RefreshTasks;
import sami.event.ReturnValue;
import sami.event.SendAbortMission;
import sami.event.SendProxyAbortAllMissions;
import sami.event.SendProxyAbortFutureMissions;
import sami.event.StartTimer;
import sami.event.TaskComplete;
import sami.event.TaskStarted;
import sami.event.TimerExpired;
import sami.event.TokensReturned;
import sami.mission.MissionPlanSpecification;
import sami.mission.Token;
import sami.proxy.ProxyInt;
import sami.service.information.InformationServer;
import sami.service.information.InformationServiceProviderInt;

/**
 *
 * @author nbb
 */
public class CoreEventHandler implements EventHandlerInt, InformationServiceProviderInt {

    private static final Logger LOGGER = Logger.getLogger(CoreEventHandler.class.getName());
    final ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();
    HashMap<UUID, Timer> timerLookup = new HashMap<UUID, Timer>();
    HashMap<UUID, HashMap<ProxyInt, Timer>> proxyTimerLookup = new HashMap<UUID, HashMap<ProxyInt, Timer>>();

    public CoreEventHandler() {
        InformationServer.addServiceProvider(this);
    }

    @Override
    public void invoke(final OutputEvent oe, ArrayList<Token> tokens) {
        LOGGER.log(Level.FINE, "CoreEventHandler invoked with " + oe);
        if (oe instanceof AbortMission) {
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    token.getProxy().abortMission(oe.getMissionId());
                }
            }
        } else if (oe instanceof CompleteMission) {
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    token.getProxy().completeMission(oe.getMissionId());
                }
            }
        } else if (oe instanceof SendProxyAbortAllMissions) {
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    // Add proxy that will be aborting the missions
                    ArrayList<ProxyInt> relevantProxies = new ArrayList<ProxyInt>();
                    relevantProxies.add(token.getProxy());
                    // Get list of all mission ids proxy is involved in
                    ArrayList<UUID> missionIds = new ArrayList<UUID>();
                    ArrayList<OutputEvent> outputEvents = token.getProxy().getEvents();
                    for (OutputEvent proxyOe : outputEvents) {
                        if (!missionIds.contains(proxyOe.getMissionId())) {
                            missionIds.add(proxyOe.getMissionId());

                            // Check that PM has an active ProxyAbortMissionReceived that will handle this
                            //  Shared SMs may not currently have any active places, so the IE will not trigger a transition
                            //  Non-shared SMs should have an active place which will trigger a transition, but check just in case
                            PlanManager eventPm = Engine.getInstance().getPlanManager(proxyOe.getMissionId());
                            if (eventPm == null) {
                                //severe
                                continue;
                            }
                            boolean found = false;
                            ArrayList<InputEvent> eventPmActiveInputEvents = eventPm.getActiveInputEventsClone();
                            for (InputEvent ie : eventPmActiveInputEvents) {
                                if (ie instanceof ProxyAbortMissionReceived) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                // No active AbortMissionReceived in the SM
                                if (!eventPm.getIsSharedSm()) {
                                    LOGGER.severe("Want to send ProxyAbortMissionReceived to non-shared SM, but it has no active IE, manually aborting proxy from SM");
                                }
                                        // Abort this PM manually
                                //@todo this only goes one layer deep - what if SM has SM also without ProxyAbortMissionReceived?
                                token.getProxy().abortMission(eventPm.missionId);
                            } else {
                                eventPm.eventGenerated(new ProxyAbortMissionReceived(eventPm.missionId, relevantProxies));
                            }
                        }
                    }
                    // Send proxy abort mission for each mission
                    for (UUID missionId : missionIds) {
                        ProxyAbortMissionReceived pamr = new ProxyAbortMissionReceived(missionId, relevantProxies);
                        ArrayList<GeneratedEventListenerInt> listenersCopy;
                        synchronized (listeners) {
                            listenersCopy = (ArrayList<GeneratedEventListenerInt>) listeners.clone();
                        }
                        for (GeneratedEventListenerInt listener : listenersCopy) {
                            listener.eventGenerated(pamr);
                        }
                    }
                }
            }
        } else if (oe instanceof SendProxyAbortFutureMissions) {
            for (Token token : tokens) {
                if (token.getProxy() != null) {
                    // Add proxy that will be aborting the missions
                    ArrayList<ProxyInt> relevantProxies = new ArrayList<ProxyInt>();
                    relevantProxies.add(token.getProxy());
                    // Get list of all mission ids proxy is involved in
                    ArrayList<UUID> missionIds = new ArrayList<UUID>();
                    ArrayList<OutputEvent> outputEvents = token.getProxy().getEvents();
                    for (OutputEvent proxyOe : outputEvents) {
                        if (!missionIds.contains(proxyOe.getMissionId())) {
                            missionIds.add(proxyOe.getMissionId());
                        }
                    }
                    // Remove current mission from list of missions to be aborted by proxy
                    OutputEvent curOe = token.getProxy().getCurrentEvent();
                    if (curOe != null) {
                        missionIds.remove(curOe);
                    }
                    // Send proxy abort mission for each mission
                    for (UUID missionId : missionIds) {
                        ProxyAbortMissionReceived pamr = new ProxyAbortMissionReceived(missionId, relevantProxies);
                        ArrayList<GeneratedEventListenerInt> listenersCopy;
                        synchronized (listeners) {
                            listenersCopy = (ArrayList<GeneratedEventListenerInt>) listeners.clone();
                        }
                        for (GeneratedEventListenerInt listener : listenersCopy) {
                            listener.eventGenerated(pamr);
                        }
                    }
                }
            }
        } else if (oe instanceof SendAbortMission) {
            // We will move all tokens out of all places in the plan and be in an end Recovery place
            //  Do nothing (abort mission is handled by PlanManager)
            AbortMissionReceived amr = new AbortMissionReceived(oe.getMissionId());
            ArrayList<GeneratedEventListenerInt> listenersCopy;
            synchronized (listeners) {
                listenersCopy = (ArrayList<GeneratedEventListenerInt>) listeners.clone();
            }
            for (GeneratedEventListenerInt listener : listenersCopy) {
                listener.eventGenerated(amr);
            }
        } else if (oe instanceof StartTimer) {
            // When looping back via non TimerExpired branch and starting a new StartTimer, first one could expire and trigger 
            // Solution: OE UUID -> Timer lookup, kill/modify it if it already exists
            if (oe.getId() == null) {
                oe.setId(UUID.randomUUID());
                LOGGER.severe("StartTimer OE had no OE ID, created one: " + oe.getId());
            }
            if (timerLookup.containsKey(oe.getId())) {
                timerLookup.get(oe.getId()).stop();
            }
            Timer timer = new Timer(((StartTimer) oe).timerDuration * 1000, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    TimerExpired te = new TimerExpired(oe.getId(), oe.getMissionId());
                    ArrayList<GeneratedEventListenerInt> listenersCopy;
                    synchronized (listeners) {
                        listenersCopy = (ArrayList<GeneratedEventListenerInt>) listeners.clone();
                    }
                    for (GeneratedEventListenerInt listener : listenersCopy) {
                        listener.eventGenerated(te);
                    }
                }
            });
            timerLookup.put(oe.getId(), timer);
            timer.setRepeats(false);
            timer.start();
        } else if (oe instanceof ProxyStartTimer) {
            // When looping back via non ProxyTimerExpired branch and starting a new ProxyStartTimer, first one could expire and trigger 
            // Solution: OE UUID -> Proxy -> Timer lookup, kill/modify it if it already exists
            if (oe.getId() == null) {
                oe.setId(UUID.randomUUID());
                LOGGER.severe("ProxyStartTimer OE had no OE ID, created one: " + oe.getId());
            }
            HashMap<ProxyInt, Timer> lookup;
            if (proxyTimerLookup.containsKey(oe.getId())) {
                lookup = proxyTimerLookup.get(oe.getId());
            } else {
                lookup = new HashMap<ProxyInt, Timer>();
                proxyTimerLookup.put(oe.getId(), lookup);
            }
            for (final Token token : tokens) {
                if (token.getProxy() != null) {
                    if (lookup.containsKey(token.getProxy())) {
                        lookup.get(token.getProxy()).stop();
                    }
                    Timer timer = new Timer(((ProxyStartTimer) oe).timerDuration * 1000, new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            ArrayList<ProxyInt> expiredProxies = new ArrayList<ProxyInt>();
                            expiredProxies.add(token.getProxy());
                            ProxyTimerExpired pte = new ProxyTimerExpired(oe.getId(), oe.getMissionId(), expiredProxies);
                            ArrayList<GeneratedEventListenerInt> listenersCopy;
                            synchronized (listeners) {
                                listenersCopy = (ArrayList<GeneratedEventListenerInt>) listeners.clone();
                            }
                            for (GeneratedEventListenerInt listener : listenersCopy) {
                                listener.eventGenerated(pte);
                            }
                        }
                    });
                    lookup.put(token.getProxy(), timer);
                    timer.setRepeats(false);
                    timer.start();
                }
            }
        } else if (oe instanceof ReturnValue) {
            ReturnValue returnValue = (ReturnValue) oe;
            PlanManager pm = Engine.getInstance().getPlanManager(returnValue.getMissionId());
            String returnVariableName = pm.getPlanName() + MissionPlanSpecification.RETURN_SUFFIX;
            // Set local scope
            Engine.getInstance().setVariableValue(returnVariableName, returnValue.getReturnValue(), pm);
            // Set in parent's scope
            PlanManager parentPm = Engine.getInstance().getParentPm(pm);
            if (parentPm != null) {
                Engine.getInstance().setVariableValue(returnVariableName, returnValue.getReturnValue(), parentPm);
            } else {
                LOGGER.warning("ReturnValue on event in mission with no parent mission.");
            }
        } else if (oe instanceof RefreshTasks) {
            for (Token token : tokens) {
                if (token.getType() == Token.TokenType.Task) {
                    if (token.getTask() != null && token.getProxy() != null) {
                        if (token.getProxy().getCurrentTask() == token.getTask()) {
                            // Generate task started for every proxy
                            ArrayList<ProxyInt> proxyList = Engine.getInstance().getProxyServer().getProxyListClone();
                            for (ProxyInt proxy : proxyList) {
                                Task currentTask = proxy.getCurrentTask();
                                if (currentTask != null) {
                                    PlanManager pm = Engine.getInstance().getPlanManager(currentTask);
                                    if (pm != null) {
                                        pm.eventGenerated(new TaskStarted(pm.missionId, currentTask));
                                    } else {
                                        LOGGER.severe("No mapping from task to plan manager: " + currentTask);
                                    }
                                }
                            }
                        }
                    } else if (token.getTask() == null) {
                        LOGGER.warning("RefreshTasks activated with task token with a null task: " + token);
                    } else {
                        LOGGER.warning("RefreshTasks activated with task token with a null proxy: " + token);
                    }
                }
            }
        } else if (oe instanceof TaskComplete) {
            for (Token token : tokens) {
                if (token.getType() == Token.TokenType.Task) {
                    if (token.getTask() != null && token.getProxy() != null) {
                        // Update proxy's task list
                        token.getProxy().taskCompleted(token.getTask());
                        token.setProxy(null);
                        Engine.getInstance().unlinkTask(token.getTask());
                    } else if (token.getTask() == null) {
                        LOGGER.warning("TaskComplete activated with task token with a null task: " + token);
                    } else {
                        LOGGER.warning("TaskComplete activated with task token with a null proxy: " + token);
                    }
                }
            }
        } else if (oe instanceof GetAllProxyTokens) {
            TokensReturned tr = new TokensReturned(oe.getId(), oe.getMissionId(), Engine.getInstance().getAllProxies());
            ArrayList<GeneratedEventListenerInt> listenersCopy;
            synchronized (listeners) {
                listenersCopy = (ArrayList<GeneratedEventListenerInt>) listeners.clone();
            }
            for (GeneratedEventListenerInt listener : listenersCopy) {
                listener.eventGenerated(tr);
            }
        }
    }

    /**
     * Offer a plan's IE subscription to the handler Always keep plans
     * registered to CoreEventHandler
     *
     * @param sub The newly created input event subscription
     * @return If the handler accepts the subscription
     */
    @Override
    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "CoreEventHandler offered subscription: " + sub);
        synchronized (listeners) {
            if (sub.getSubscriptionClass() == AbortMissionReceived.class
                    || sub.getSubscriptionClass() == ProxyAbortMissionReceived.class
                    || sub.getSubscriptionClass() == TimerExpired.class
                    || sub.getSubscriptionClass() == ProxyTimerExpired.class
                    || sub.getSubscriptionClass() == TokensReturned.class) {
                LOGGER.log(Level.FINE, "\tCoreEventHandler took subscription request: " + sub);
                if (!listeners.contains(sub.getListener())) {
                    LOGGER.log(Level.FINE, "\t\tCoreEventHandler adding listener: " + sub.getListener());
                    listeners.add(sub.getListener());
                    listenerGCCount.put(sub.getListener(), 1);
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Cancel a plan's IE subscription Always keep plans registered to
     * CoreEventHandler
     *
     * @param sub The subscription to cancel due to event completion or
     * cancellation
     * @return If the handler held and canceled this subscription
     */
    @Override
    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "CoreEventHandler asked to cancel subscription: " + sub);
        synchronized (listeners) {
            if ((sub.getSubscriptionClass() == AbortMissionReceived.class
                    || sub.getSubscriptionClass() == ProxyAbortMissionReceived.class
                    || sub.getSubscriptionClass() == TimerExpired.class
                    || sub.getSubscriptionClass() == ProxyTimerExpired.class
                    || sub.getSubscriptionClass() == TokensReturned.class)
                    && listeners.contains(sub.getListener())) {
                return true;
            }
            return false;
        }
    }
}
