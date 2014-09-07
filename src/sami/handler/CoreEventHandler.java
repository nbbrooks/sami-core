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
import sami.event.GeneratedEventListenerInt;
import sami.event.GeneratedInputEventSubscription;
import sami.event.OutputEvent;
import sami.event.ProxyAbortMissionReceived;
import sami.event.RefreshTasks;
import sami.event.ReturnValue;
import sami.event.SendAbortMission;
import sami.event.SendProxyAbortAllMissions;
import sami.event.SendProxyAbortFutureMissions;
import sami.event.StartTimer;
import sami.event.TaskComplete;
import sami.event.TaskStarted;
import sami.event.TimerExpired;
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
    ArrayList<GeneratedEventListenerInt> listeners = new ArrayList<GeneratedEventListenerInt>();
    HashMap<GeneratedEventListenerInt, Integer> listenerGCCount = new HashMap<GeneratedEventListenerInt, Integer>();

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
                        }
                    }
                    // Send proxy abort mission for each mission
                    for (UUID missionId : missionIds) {
                        ProxyAbortMissionReceived pamr = new ProxyAbortMissionReceived(missionId, relevantProxies);
                        for (GeneratedEventListenerInt listener : listeners) {
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
                        for (GeneratedEventListenerInt listener : listeners) {
                            listener.eventGenerated(pamr);
                        }
                    }
                }
            }
        } else if (oe instanceof SendAbortMission) {
            // We will move all tokens out of all places in the plan and be in an end Recovery place
            //  Do nothing (abort mission is handled by PlanManager)
            AbortMissionReceived amr = new AbortMissionReceived(oe.getMissionId());
            for (GeneratedEventListenerInt listener : listeners) {
                listener.eventGenerated(amr);
            }
        } else if (oe instanceof StartTimer) {
            Timer timer = new Timer(((StartTimer) oe).timerDuration * 1000, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    TimerExpired te = new TimerExpired(oe.getId(), oe.getMissionId());
                    for (GeneratedEventListenerInt listener : listeners) {
                        listener.eventGenerated(te);
                    }
                }
            });
            timer.setRepeats(false);
            timer.start();
        } else if (oe instanceof ReturnValue) {
            ReturnValue returnValue = (ReturnValue) oe;
            PlanManager pm = Engine.getInstance().getPlanManager(returnValue.getMissionId());
            String returnVariableName = pm.getPlanName() + MissionPlanSpecification.RETURN_SUFFIX;
            Engine.getInstance().setVariableValue(returnVariableName, returnValue.getReturnValue());
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
        }
    }

    @Override
    public boolean offer(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "CoreEventHandler offered subscription: " + sub);
        if (sub.getSubscriptionClass() == AbortMissionReceived.class
                || sub.getSubscriptionClass() == TimerExpired.class) {
            LOGGER.log(Level.FINE, "\tCoreEventHandler took subscription request: " + sub);
            if (!listeners.contains(sub.getListener())) {
                LOGGER.log(Level.FINE, "\t\tCoreEventHandler adding listener: " + sub.getListener());
                listeners.add(sub.getListener());
                listenerGCCount.put(sub.getListener(), 1);
            } else {
                LOGGER.log(Level.FINE, "\t\tCoreEventHandler incrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) + 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean cancel(GeneratedInputEventSubscription sub) {
        LOGGER.log(Level.FINE, "CoreEventHandler asked to cancel subscription: " + sub);
        if ((sub.getSubscriptionClass() == AbortMissionReceived.class
                || sub.getSubscriptionClass() == TimerExpired.class)
                && listeners.contains(sub.getListener())) {
            LOGGER.log(Level.FINE, "CoreEventHandler  canceling subscription: " + sub);
            if (listenerGCCount.get(sub.getListener()) == 1) {
                // Remove listener
                LOGGER.log(Level.FINE, "\tCoreEventHandler removing listener: " + sub.getListener());
                listeners.remove(sub.getListener());
                listenerGCCount.remove(sub.getListener());
            } else {
                // Decrement garbage colleciton count
                LOGGER.log(Level.FINE, "\t\tCoreEventHandler decrementing listener: " + sub.getListener());
                listenerGCCount.put(sub.getListener(), listenerGCCount.get(sub.getListener()) - 1);
            }
            return true;
        }
        return false;
    }
}
