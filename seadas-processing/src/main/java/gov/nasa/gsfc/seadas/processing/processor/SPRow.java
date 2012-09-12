package gov.nasa.gsfc.seadas.processing.processor;

import gov.nasa.gsfc.seadas.processing.core.L2genData;
import gov.nasa.gsfc.seadas.processing.core.ParamInfo;
import gov.nasa.gsfc.seadas.processing.core.ParamList;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.general.CloProgramUI;
import gov.nasa.gsfc.seadas.processing.general.GridBagConstraintsCustom;
import gov.nasa.gsfc.seadas.processing.general.ProgramUIFactory;
import gov.nasa.gsfc.seadas.processing.general.SeadasLogger;
import gov.nasa.gsfc.seadas.processing.l2gen.userInterface.L2genForm;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: dshea
 * Date: 8/21/12
 * Time: 8:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class SPRow {
    public static final String PARAM_STRING_EVENT = "paramString";
    public static final String KEEPFILES_PARAM = "keepfiles";

    private String name;
    private CloProgramUI cloProgramUI;
    private SPForm parentForm;

    private JLabel nameLabel;
    private JCheckBox keepCheckBox;
    private JTextField paramTextField;
    private JButton configButton;
    private JPanel configPanel;
    private ParamList paramList;
    private SwingPropertyChangeSupport propertyChangeSupport;


    public SPRow(String name, SPForm parentForm) {
        this.name = name;
        this.parentForm = parentForm;

        propertyChangeSupport = new SwingPropertyChangeSupport(this);
        paramList = new ParamList();
        nameLabel = new JLabel(name);
        keepCheckBox = new JCheckBox();
        keepCheckBox.setSelected(false);
        keepCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                handleKeepCheckBox();
            }
        });
        paramTextField = new JTextField();
        paramTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleParamTextField();
            }
        });
        paramTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                handleParamTextField();
            }
        });

        configButton = new JButton("...");
        configButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleButtonEvent();
            }
        });

    }

    public String getName() {
        return name;
    }

    // this method assumes the the JPanel passed in is using a grid bag layout
    public void attachComponents(JPanel base, int row) {
        base.add(nameLabel,
                new GridBagConstraintsCustom(0, row, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        base.add(keepCheckBox,
                new GridBagConstraintsCustom(1, row, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
        base.add(paramTextField,
                new GridBagConstraintsCustom(2, row, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        base.add(configButton,
                new GridBagConstraintsCustom(3, row, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
    }

    private void createConfigPanel() {
        if (configPanel == null) {
            if (name.equals("main")) {
                cloProgramUI = new ProgramUIFactory("seadas_processor.py", "seadas_processor.xml");
                configPanel = (JPanel)cloProgramUI;
            } else if (name.equals("l2gen")) {
                cloProgramUI = new L2genForm(parentForm.getAppContext(), "l2gen.xml", L2genData.installTinyIFile(), false);
                configPanel = cloProgramUI.getParamPanel();
            } else {
                String xmlFile = name.replace(".py", "").concat(".xml");
                cloProgramUI = new ProgramUIFactory(name, xmlFile);
                configPanel = cloProgramUI.getParamPanel();
            }

            // set parameters to default values
            getParamListFromCloProgramUI();
            paramList.setParamString("");
        }
    }

    private void getParamListFromCloProgramUI() {
        paramList = (ParamList) cloProgramUI.getProcessorModel().getParamList().clone();
        cleanIOParams(paramList);
        if (keepCheckBox.isSelected()) {
            paramList.addInfo(new ParamInfo(KEEPFILES_PARAM, ParamInfo.BOOLEAN_TRUE, ParamInfo.Type.BOOLEAN, ParamInfo.BOOLEAN_FALSE));
        } else {
            paramList.addInfo(new ParamInfo(KEEPFILES_PARAM, ParamInfo.BOOLEAN_FALSE, ParamInfo.Type.BOOLEAN, ParamInfo.BOOLEAN_FALSE));
        }
    }

    private void handleKeepCheckBox() {
        createConfigPanel();
        if (keepCheckBox.isSelected() != paramList.isValueTrue(KEEPFILES_PARAM)) {
            String oldParamString = getParamString();
            if (keepCheckBox.isSelected()) {
                paramList.setValue(KEEPFILES_PARAM, ParamInfo.BOOLEAN_TRUE);
            } else {
                paramList.setValue(KEEPFILES_PARAM, ParamInfo.BOOLEAN_FALSE);
            }
            String str = getParamString();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }

    private void handleParamTextField() {
        createConfigPanel();
        String oldParamString = getParamString();
        String str = paramTextField.getText();
        ParamInfo param = paramList.getInfo(KEEPFILES_PARAM);
        if (param != null) {
            str = str + " " + param.getParamString();
        }
        paramList.setParamString(str);
        str = getParamString();
        updateParamTextField();
        if (!oldParamString.equals(str)) {
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }

    private void handleButtonEvent() {
        createConfigPanel();

        final Window parent = parentForm.getAppContext().getApplicationWindow();
        final ModalDialog modalDialog = new ModalDialog(parent, name, configPanel, ModalDialog.ID_OK_CANCEL_HELP, name);

        // modalDialog.getButton(ModalDialog.ID_OK).setEnabled(true);
        modalDialog.getButton(ModalDialog.ID_OK).setText("Save");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("");
        modalDialog.getButton(ModalDialog.ID_HELP).setIcon(UIUtils.loadImageIcon("icons/Help24.gif"));

        //Make sure program is only executed when the "run" button is clicked.
        ((JButton) modalDialog.getButton(ModalDialog.ID_OK)).setDefaultCapable(false);
        modalDialog.getJDialog().getRootPane().setDefaultButton(null);

        // load the UI with the current param values
        ParamList list = (ParamList) paramList.clone();
        list.removeInfo(KEEPFILES_PARAM);
        cloProgramUI.setParamString(list.getParamString("\n"));
        String oldUIParamString = cloProgramUI.getParamString();

        final int dialogResult = modalDialog.show();

        SeadasLogger.getLogger().info("dialog result: " + dialogResult);

        if (dialogResult != ModalDialog.ID_OK) {
            return;
        }

        String str = cloProgramUI.getParamString();
        if (!oldUIParamString.equals(str)) {
            getParamListFromCloProgramUI();
            updateParamList();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldUIParamString, str);
        }

    }

    private void updateKeepCheckbox() {
        keepCheckBox.setSelected(paramList.isValueTrue(KEEPFILES_PARAM));
    }

    private void updateParamTextField() {
        ParamList list = (ParamList) paramList.clone();
        list.removeInfo(KEEPFILES_PARAM);
        paramTextField.setText(list.getParamString(" "));
    }

    private void updateParamList() {
        updateParamTextField();
        updateKeepCheckbox();
    }

    public ParamList getParamList() {
        return paramList;
    }

    public String getParamString(String separator) {
        return paramList.getParamString(separator);
    }

    public String getParamString() {
        return paramList.getParamString();
    }

    public void setParamString(String str) {
        String oldParamString = getParamString();
        paramList.setParamString(str);
        str = getParamString();
        if (!oldParamString.equals(str)) {
            updateParamList();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }

    public void setParamValue(String name, String str) {
        String oldParamString = getParamString();
        paramList.setValue(name, str);
        str = getParamString();
        if (!oldParamString.equals(str)) {
            updateParamList();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }


    private void cleanIOParams(ParamList list) {
        if (name.equals("main")) {
            return;
        }

        String[] IOParams = {"ifile", "ofile", "infile", "geofile"};

        for (String IOParam : IOParams) {
            ParamInfo param = list.getInfo(IOParam);
            if (param != null) {
                list.setValue(param.getName(), param.getDefaultValue());
            }
        }
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }


}
