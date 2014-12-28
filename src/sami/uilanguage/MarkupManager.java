package sami.uilanguage;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.Border;
import sami.engine.Engine;
import sami.markup.Attention;
import sami.markup.Markup;
import sami.markup.MixedInitiativeTrigger;
import sami.uilanguage.fromui.FromUiMessage;
import sami.uilanguage.fromui.FromUiMessageGenerator;
import sami.uilanguage.toui.SelectionMessage;
import sami.uilanguage.toui.ToUiMessage;

/**
 *
 * @author nbb
 */
public class MarkupManager {

    private final static Logger LOGGER = Logger.getLogger(MarkupManager.class.getName());
    ToUiMessage toUiMessage;
    // Attention
    private final Object attentionLock = new Object();
    MouseAdapter attentionAdapter;
    boolean needsAttention = false;
    Timer blinkTimer;
    // MixedInitiativeTrigger
    Timer autonomyTimer;
    //
    JFrame frame;
    ArrayList<MarkupComponent> components = new ArrayList<MarkupComponent>();
    ArrayList<JPanel> panels = new ArrayList<JPanel>();
    ArrayList<JFrame> frames = new ArrayList<JFrame>();

    public MarkupManager(ToUiMessage toUiMessage) {
        this.toUiMessage = toUiMessage;
        handleMarkups();
    }

    public void addComponent(final MarkupComponent component) {
        if (!components.contains(component)) {
            components.add(component);

//            for (Markup markup : toUiMessage.getMarkups()) {
//                if (markup instanceof Attention) {
//                    Attention attention = (Attention) markup;
//                    if (attention.attentionTarget == Attention.AttentionTarget.PANEL) {
//                        // Blink or highlight?
//                        if (attention.attentionType == Attention.AttentionType.BLINK) {
//                            blinkTarget((JComponent) frame.getContentPane(), attention);
//                        } else if (attention.attentionType == Attention.AttentionType.HIGHLIGHT) {
//                            highlightTarget((JComponent) frame.getContentPane(), attention);
//                        }
//                        // Stop on click?
//                        if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//                            frame.getGlassPane().addMouseListener(attentionAdapter);
//                        }
//                    }
//                }
//            }
            component.handleMarkups(toUiMessage.getMarkups(), this);
        }
//        if (component instanceof UiFrame) {
//            addFrame((UiFrame) component);
//        }
//        if (component instanceof UiPanel) {
//            addPanel((UiPanel) component);
//        }
    }

//    public void addFrame(final UiFrame frame) {
//        if (!frames.contains(frame)) {
//            System.out.println("### MarkupManager adding UiFrame: " + frame);
//            frames.add(frame);
//
//            for (Markup markup : toUiMessage.getMarkups()) {
//                if (markup instanceof Attention) {
//                    Attention attention = (Attention) markup;
//                    if (attention.attentionTarget == Attention.AttentionTarget.FRAME) {
//                        // Blink or highlight?
//                        if (attention.attentionType == Attention.AttentionType.BLINK) {
//                            blinkFrame(frame, attention);
//                        } else if (attention.attentionType == Attention.AttentionType.HIGHLIGHT) {
//                            highlightTarget((JComponent) frame.getContentPane(), attention);
//                        }
//                        // Stop on click?
//                        if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//                            frame.getGlassPane().addMouseListener(attentionAdapter);
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    public void addPanel(UiPanel panel) {
//        if (!panels.contains(panel)) {
//            System.out.println("### MarkupManager adding UiPanel: " + panel);
//            panels.add(panel);
//
//            for (Markup markup : toUiMessage.getMarkups()) {
//                if (markup instanceof Attention) {
//                    Attention attention = (Attention) markup;
//                    if (attention.attentionTarget == Attention.AttentionTarget.PANEL) {
//                        // Blink or highlight?
//                        if (attention.attentionType == Attention.AttentionType.BLINK) {
//                            blinkPanel(panel, attention);
//                        } else if (attention.attentionType == Attention.AttentionType.HIGHLIGHT) {
//                            highlightTarget(panel, attention);
//                        }
//                        // Stop on click?
//                        if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//                            panel.addMouseListener(attentionAdapter);
//                        }
//                    }
//                }
//            }
//        }
//    }
    private void handleMarkups() {
        for (Markup markup : toUiMessage.getMarkups()) {
            if (markup instanceof Attention) {
                Attention attention = (Attention) markup;
                if (attention.attentionType == Attention.AttentionType.BLINK) {
//                    System.out.println("### Starting blink timer");
                    blinkTimer = new Timer(attention.blink.cycleLength / 2, null);
                    blinkTimer.setActionCommand("1");
                    blinkTimer.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
//                            System.out.println("### Blink timer " + blinkTimer.getActionCommand());
                            // Toggle value so we can synchronize blinking
                            blinkTimer.setActionCommand(blinkTimer.getActionCommand().equals("1") ? "0" : "1");
                        }
                    });
                    blinkTimer.start();
                }
                if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//                    System.out.println("### Needs attention");
                    synchronized (attentionLock) {
                        needsAttention = true;
                    }
                }
//                    if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//                        if(attention.attentionTarget == Attention.AttentionTarget.FRAME) {
//                            
//                        } else if(attention.attentionTarget == Attention.AttentionTarget.PANEL) {
//                        }
//                        attentionAdapter = new MouseAdapter() {
//                            @Override
//                            public void mousePressed(MouseEvent e) {
//                                synchronized (attentionLock) {
//                                    needsAttention = false;
//                                    blinkTimer.stop();
//                                    ((JComponent) frame.getContentPane()).setBorder(null);
//                                }
//                            }
//                        };
//
//                        synchronized (attentionLock) {
//                            needsAttention = true;
//                        }
//                    }
//                } else if (attention.attentionType == Attention.AttentionType.HIGHLIGHT) {
//                    if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//
//                        System.out.println("### Creating click listener");
//                        attentionAdapter = new MouseAdapter() {
//                            @Override
//                            public void mousePressed(MouseEvent e) {
//                                synchronized (attentionLock) {
//                                    needsAttention = false;
//                                    blinkTimer.stop();
//                                    ((JComponent) frame.getContentPane()).setBorder(null);
//                                }
//                            }
//                        };
//
//                        synchronized (attentionLock) {
//                            needsAttention = true;
//                        }
//                    }
//                }
            } else if (markup instanceof MixedInitiativeTrigger) {
//                System.out.println("### Starting MI trigger");
                MixedInitiativeTrigger trigger = (MixedInitiativeTrigger) markup;
                if (trigger.trigger == MixedInitiativeTrigger.Trigger.IMMEDIATELY
                        || trigger.trigger == MixedInitiativeTrigger.Trigger.TIMEOUT) {
                    setTrigger((MixedInitiativeTrigger) markup);
                }
            }
        }
    }

//    public void handleMarkups() {
//        for (Markup markup : toUiMessage.getMarkups()) {
//            if (markup instanceof Attention) {
//                Attention attention = (Attention) markup;
//                if (attention.attentionType == Attention.AttentionType.BLINK) {
//                    System.out.println("### Starting blink timer");
//                    blinkTimer = new Timer(attention.blink.cycleLength / 2, null);
//                    blinkTimer.start();
//                    if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//                        System.out.println("### Creating click listener for blink");
//                        
//                        attentionAdapter = new MouseAdapter() {
//                            @Override
//                            public void mousePressed(MouseEvent e) {
//                                synchronized (attentionLock) {
//                                    needsAttention = false;
//                                    blinkTimer.stop();
//                                    ((JComponent) frame.getContentPane()).setBorder(null);
//                                }
//                            }
//                        };
//
//                        synchronized (attentionLock) {
//                            needsAttention = true;
//                        }
//                    }
//                } else if (attention.attentionType == Attention.AttentionType.HIGHLIGHT) {
//                    if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
//
//                        System.out.println("### Creating click listener");
//                        attentionAdapter = new MouseAdapter() {
//                            @Override
//                            public void mousePressed(MouseEvent e) {
//                                synchronized (attentionLock) {
//                                    needsAttention = false;
//                                    blinkTimer.stop();
//                                    ((JComponent) frame.getContentPane()).setBorder(null);
//                                }
//                            }
//                        };
//
//                        synchronized (attentionLock) {
//                            needsAttention = true;
//                        }
//                    }
//                }
//            } else if (markup instanceof MixedInitiativeTrigger) {
//                System.out.println("### Starting MI trigger");
//                MixedInitiativeTrigger trigger = (MixedInitiativeTrigger) markup;
//                if (trigger.trigger == MixedInitiativeTrigger.Trigger.IMMEDIATELY
//                        || trigger.trigger == MixedInitiativeTrigger.Trigger.TIMEOUT) {
//                    setTrigger((MixedInitiativeTrigger) markup);
//                }
//            }
//        }
//    }
    public void blinkFrame(final UiFrame target, Attention attention) {
//        System.out.println("### Blink target: " + target);
        blinkTimer.addActionListener(new ActionListener() {
            boolean blink = false;
            Border border = BorderFactory.createMatteBorder(10, 10, 10, 10, Color.YELLOW);

            @Override
            public void actionPerformed(ActionEvent e) {
                blink = !blink;
                if (blink) {
                    ((JComponent) target.getContentPane()).setBorder(border);
                } else {
                    ((JComponent) target.getContentPane()).setBorder(null);
                }
                frame.repaint();
            }
        });
    }

    public void blinkPanel(final UiPanel target, Attention attention) {
//        System.out.println("### Blink target: " + target);
        blinkTimer.addActionListener(new ActionListener() {
            boolean blink = false;
            Border border = BorderFactory.createMatteBorder(10, 10, 10, 10, Color.YELLOW);

            @Override
            public void actionPerformed(ActionEvent e) {
                blink = !blink;
                if (blink) {
                    target.setBorder(border);
                } else {
                    target.setBorder(null);
                }
                target.repaint();
            }
        });
    }

    public void highlightTarget(JComponent target, Attention attention) {
//        System.out.println("### Highlight target: " + target);
        target.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, Color.YELLOW));
    }

    public void setTrigger(MixedInitiativeTrigger trigger) {
        final FromUiMessage autonomyDecision = FromUiMessageGenerator.getInstance().getFromUiMessage((SelectionMessage) toUiMessage, 0);
        if (autonomyDecision == null) {
            LOGGER.severe("Failed to retrieve Mixed Initiative decision for UI message: " + toUiMessage);
            return;
        }

        if (trigger.trigger == MixedInitiativeTrigger.Trigger.IMMEDIATELY) {
            // Send FromUiMessage and notify listeners
            LOGGER.fine("Immediately activating autonomy for message: " + toUiMessage + " with UUID: " + toUiMessage.getRelevantOutputEventId());
            autonomyTimer = new Timer(1, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LOGGER.fine("Timer activating autonomy for message: " + toUiMessage + " with UUID: " + toUiMessage.getRelevantOutputEventId());
                    // Send FromUiMessage and notify listeners
                    if (Engine.getInstance().getUiServer() != null) {
                        Engine.getInstance().getUiServer().UIMessage(autonomyDecision);
                        Engine.getInstance().getUiClient().toUiMessageHandled(toUiMessage.getMessageId());
                    } else {
                        LOGGER.warning("NULL UiServer!");
                    }
                }
            });
            autonomyTimer.setRepeats(false);
            autonomyTimer.start();
        } else if (trigger.trigger == MixedInitiativeTrigger.Trigger.TIMEOUT) {
            autonomyTimer = new Timer(trigger.timeout.timeout * 1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LOGGER.fine("Timer activating autonomy for message: " + toUiMessage + " with UUID: " + toUiMessage.getRelevantOutputEventId());
                    // Send FromUiMessage and notify listeners
                    if (Engine.getInstance().getUiServer() != null) {
                        Engine.getInstance().getUiServer().UIMessage(autonomyDecision);
                        Engine.getInstance().getUiClient().toUiMessageHandled(toUiMessage.getMessageId());
                    } else {
                        LOGGER.warning("NULL UiServer!");
                    }
                }
            });
            LOGGER.fine("Starting autonomy timer for: " + (trigger.timeout.timeout * 1000) + " for message: " + toUiMessage + " with UUID: " + toUiMessage.getRelevantOutputEventId());
            autonomyTimer.setRepeats(false);
            autonomyTimer.start();
        }
    }

    public void markupConsumed(Markup markup) {
//        System.out.println("### Markup consumed: " + markup);
        if (markup instanceof Attention) {
            Attention attention = (Attention) markup;
            if (attention.attentionType == Attention.AttentionType.BLINK) {
                blinkTimer.stop();
            }
            needsAttention = false;
        } else if (markup instanceof MixedInitiativeTrigger) {
            autonomyTimer.stop();
        }
        for (MarkupComponent component : components) {
            component.disableMarkup(markup);
        }
    }
}
