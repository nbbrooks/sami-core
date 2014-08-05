package sami.uilanguage;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.border.Border;
import sami.markup.Attention;
import sami.markup.Attention.AttentionType;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public class UiFrame extends JFrame implements MarkupComponent {

    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    public final ArrayList<Class> widgetClasses = new ArrayList<Class>();
    Component glassPane;

    public UiFrame() {
        super();
        populateLists();
    }

    public UiFrame(String title) {
        this();
        setTitle(title);
    }

    private void populateLists() {
        // Creation
        //
        // Visualization
        //
        // Markups
        supportedMarkups.add(Attention.AttentionEnd.ON_CLICK);
        supportedMarkups.add(Attention.AttentionTarget.FRAME);
        supportedMarkups.add(Attention.AttentionType.BLINK);
        supportedMarkups.add(Attention.AttentionType.HIGHLIGHT);
    }

    @Override
    public int getCreationComponentScore(java.lang.reflect.Type type, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getSelectionComponentScore(java.lang.reflect.Type type, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMarkupScore(ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MarkupComponent useCreationComponent(java.lang.reflect.Type type, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MarkupComponent useSelectionComponent(Object selectionObject, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JComponent getComponent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getComponentValue(Class componentClass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean setComponentValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleMarkups(ArrayList<Markup> markups, final MarkupManager manager) {
        for (final Markup markup : markups) {
            if (markup instanceof Attention) {
                Attention attention = (Attention) markup;
                if (attention.attentionTarget == Attention.AttentionTarget.FRAME) {
                    if (attention.attentionType == AttentionType.BLINK) {
                        manager.blinkTimer.addActionListener(new ActionListener() {

                            Border opaqueBorder = BorderFactory.createMatteBorder(10, 10, 10, 10, Color.YELLOW);
                            Border transBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (e.getActionCommand().equals("1")) {
                                    ((JComponent) getContentPane()).setBorder(opaqueBorder);
                                } else {
                                    ((JComponent) getContentPane()).setBorder(transBorder);
                                }
                                repaint();
                            }
                        });
                    } else if (attention.attentionType == AttentionType.HIGHLIGHT) {
                        ((JComponent) getContentPane()).setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, Color.YELLOW));
                    }
                    if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
                        glassPane = getGlassPane();
                        glassPane.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mousePressed(MouseEvent e) {
                                manager.markupConsumed(markup);
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void disableMarkup(Markup markup) {
        if (markup instanceof Attention) {
            ((JComponent) getContentPane()).setBorder(null);
        }
    }

    @Override
    public ArrayList<Class> getSupportedCreationClasses() {
        ArrayList<Class> compCreationClasses = new ArrayList<Class>();
        compCreationClasses.addAll(supportedCreationClasses);
        for (Class widgetClass : widgetClasses) {
            try {
                MarkupComponentWidget temp = (MarkupComponentWidget) widgetClass.newInstance();
                ArrayList<Class> widgetCreationClasses = temp.getSupportedCreationClasses();
                for (Class widgetCreationClass : widgetCreationClasses) {
                    if (!compCreationClasses.contains(widgetCreationClass)) {
                        compCreationClasses.add(widgetCreationClass);
                    }
                }
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return compCreationClasses;
    }
}
