package sami.uilanguage;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import sami.markup.Attention;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public class UiPanel extends JPanel implements MarkupComponent {

    public final ArrayList<Class> supportedCreationClasses = new ArrayList<Class>();
    public final ArrayList<Class> supportedSelectionClasses = new ArrayList<Class>();
    public final ArrayList<Enum> supportedMarkups = new ArrayList<Enum>();
    public final ArrayList<Class> widgetClasses = new ArrayList<Class>();

    public UiPanel() {
        super();
        populateLists();
    }

    private void populateLists() {
        // Creation
        //
        // Visualization
        //
        // Markups
        supportedMarkups.add(Attention.AttentionEnd.ON_CLICK);
        supportedMarkups.add(Attention.AttentionTarget.PANEL);
        supportedMarkups.add(Attention.AttentionType.BLINK);
        supportedMarkups.add(Attention.AttentionType.HIGHLIGHT);
    }

    @Override
    public int getCreationComponentScore(Type type, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getSelectionComponentScore(Type type, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMarkupScore(ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MarkupComponent useCreationComponent(Type type, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MarkupComponent useSelectionComponent(Object selectionObject, ArrayList<Markup> markups) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JComponent getComponent() {
        return this;
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
                if (attention.attentionTarget == Attention.AttentionTarget.PANEL) {
                    if (attention.attentionType == Attention.AttentionType.BLINK) {
                        manager.blinkTimer.addActionListener(new ActionListener() {

                            Border border = BorderFactory.createMatteBorder(10, 10, 10, 10, Color.YELLOW);
                            Border transBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (e.getActionCommand().equals("1")) {
                                    setBorder(border);
                                } else {
                                    setBorder(transBorder);
                                }
                                repaint();
                            }
                        });
                    } else if (attention.attentionType == Attention.AttentionType.HIGHLIGHT) {
                        setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, Color.YELLOW));
                    }
                    if (attention.attentionEnd == Attention.AttentionEnd.ON_CLICK) {
                        addMouseListener(new MouseAdapter() {
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
            setBorder(null);
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
