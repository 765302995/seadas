package gov.nasa.obpg.seadas.sandbox.l2gen;

/**
 * Created by IntelliJ IDEA.
 * User: dshea
 * Date: 1/20/12
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;
import java.awt.*;
import java.awt.event.*;

/**
 * Maintenance tip - There were some tricks to getting this code
 * working:
 * <p/>
 * 1. You have to overwite addMouseListener() to do nothing
 * 2. You have to add a mouse event on mousePressed by calling
 * super.addMouseListener()
 * 3. You have to replace the UIActionMap for the keyboard event
 * "pressed" with your own one.
 * 4. You have to remove the UIActionMap for the keyboard event
 * "released".
 * 5. You have to grab focus when the next state is entered,
 * otherwise clicking on the component won't get the focus.
 * 6. You have to make a TristateDecorator as a button model that
 * wraps the original button model and does state management.
 */
public class TristateCheckBox extends JCheckBox {
    /**
     * This is a type-safe enumerated type
     */
    public static class State {
        private State() {
        }
    }

    public static final State NOT_SELECTED = new State();
    public static final State PARTIAL = new State();
    public static final State SELECTED = new State();

    private final TristateDecorator model;

    public TristateCheckBox(String text, Icon icon, State initial) {
        super(text, icon);
        // Add a listener for when the mouse is pressed
        super.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                grabFocus();
                model.nextState();
            }
        });
        // Reset the keyboard action map
        ActionMap map = new ActionMapUIResource();
        map.put("pressed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                grabFocus();
                model.nextState();
            }
        });
        map.put("released", null);
        SwingUtilities.replaceUIActionMap(this, map);
        // set the model to the adapted model
        model = new TristateDecorator(getModel());
        setModel(model);
        setState(initial);
    }

    public TristateCheckBox(String text, State initial) {
        this(text, null, initial);
    }

    public TristateCheckBox(String text) {
        this(text, PARTIAL);
    }

    public TristateCheckBox() {
        this(null);
    }

    /**
     * No one may add mouse listeners, not even Swing!
     */
    @Override
    public void addMouseListener(MouseListener l) {
    }

    /**
     * Set the new state to either SELECTED, NOT_SELECTED or
     * PARTIAL.  If state == null, it is treated as PARTIAL.
     */
    public void setState(State state) {
        model.setState(state);
    }

    /**
     * Return the current state, which is determined by the
     * selection status of the model.
     */
    public State getState() {
        return model.getState();
    }

    @Override
    public void setSelected(boolean b) {
        if (b) {
            setState(SELECTED);
        } else {
            setState(NOT_SELECTED);
        }
    }

    /**
     * Exactly which Design Pattern is this?  Is it an Adapter,
     * a Proxy or a Decorator?  In this case, my vote lies with the
     * Decorator, because we are extending functionality and
     * "decorating" the original model with a more powerful model.
     */
    private class TristateDecorator implements ButtonModel {
                private final ButtonModel other;
        private State state;
        private Color normalBG;
        private Color partialBG = Color.green;

        private TristateDecorator(ButtonModel other) {
            this.other = other;
            normalBG = getBackground();
        }

        private void setState(State state) {
            this.state = state;
            if (state == NOT_SELECTED) {
                setBackground(normalBG);
                other.setArmed(false);
                setPressed(false);
                setSelected(false);
            } else if (state == SELECTED) {
                setBackground(normalBG);
                other.setArmed(false);
                setPressed(false);
                setSelected(true);
            } else { // either "null" or PARTIAL
                setBackground(partialBG);
                other.setArmed(true);
                setPressed(true);
                setSelected(false);
            }
        }

        /**
         * The current state is embedded in the selection / armed
         * state of the model.
         * <p/>
         * We return the SELECTED state when the checkbox is selected
         * but not armed, PARTIAL state when the checkbox is
         * selected and armed (grey) and NOT_SELECTED when the
         * checkbox is deselected.
         */
        private State getState() {
            return state;
        }

        /**
         * We rotate between NOT_SELECTED, PARTIAL, and SELECTED.
         */
        private void nextState() {
            State current = getState();
            if (current == NOT_SELECTED) {
                setState(PARTIAL);
            } else if (current == PARTIAL) {
                setState(SELECTED);
            } else if (current == SELECTED) {
                setState(NOT_SELECTED);
            }
        }

        /**
         * Filter: No one may change the armed status except us.
         */
        public void setArmed(boolean b) {
        }

        /**
         * We disable focusing on the component when it is not
         * enabled.
         */
        public void setEnabled(boolean b) {
            setFocusable(b);
            other.setEnabled(b);
        }

        /**
         * All these methods simply delegate to the "other" model
         * that is being decorated.
         */
        public boolean isArmed() {
            return other.isArmed();
        }

        public boolean isSelected() {
            return other.isSelected();
        }

        public boolean isEnabled() {
            return other.isEnabled();
        }

        public boolean isPressed() {
            return other.isPressed();
        }

        public boolean isRollover() {
            return other.isRollover();
        }

        public void setSelected(boolean b) {
            other.setSelected(b);
        }

        public void setPressed(boolean b) {
            other.setPressed(b);
        }

        public void setRollover(boolean b) {
            other.setRollover(b);
        }

        public void setMnemonic(int key) {
            other.setMnemonic(key);
        }

        public int getMnemonic() {
            return other.getMnemonic();
        }

        public void setActionCommand(String s) {
            other.setActionCommand(s);
        }

        public String getActionCommand() {
            return other.getActionCommand();
        }

        public void setGroup(ButtonGroup group) {
            other.setGroup(group);
        }

        public void addActionListener(ActionListener l) {
            other.addActionListener(l);
        }

        public void removeActionListener(ActionListener l) {
            other.removeActionListener(l);
        }

        public void addItemListener(ItemListener l) {
            other.addItemListener(l);
        }

        public void removeItemListener(ItemListener l) {
            other.removeItemListener(l);
        }

        public void addChangeListener(ChangeListener l) {
            other.addChangeListener(l);
        }

        public void removeChangeListener(ChangeListener l) {
            other.removeChangeListener(l);
        }

        public Object[] getSelectedObjects() {
            return other.getSelectedObjects();
        }
    }
}