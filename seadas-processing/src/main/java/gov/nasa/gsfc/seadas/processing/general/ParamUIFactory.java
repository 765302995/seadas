package gov.nasa.gsfc.seadas.processing.general;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import gov.nasa.gsfc.seadas.processing.core.*;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 6/7/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParamUIFactory {

    ProcessorModel processorModel;

    private String emptySpace = "  ";

    public ParamUIFactory(ProcessorModel pm) {
        this.processorModel = pm;
    }

    public JPanel createParamPanel() {
        //final JScrollPane textScrollPane = new JScrollPane(parameterTextArea);
        final JScrollPane textScrollPane = new JScrollPane(createParamPanel(processorModel));

        textScrollPane.setPreferredSize(new Dimension(700, 400));

        final JPanel parameterComponent = new JPanel(new BorderLayout());

        parameterComponent.add(textScrollPane, BorderLayout.CENTER);


        parameterComponent.setPreferredSize(parameterComponent.getPreferredSize());

        if (processorModel.getProgramName().indexOf("smigen") != -1) {
            SMItoPPMUI smItoPPMUI = new SMItoPPMUI(processorModel);
            JPanel smitoppmPanel = smItoPPMUI.getSMItoPPMPanel();
            parameterComponent.add(smitoppmPanel, BorderLayout.SOUTH);
            smitoppmPanel.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    parameterComponent.validate();
                    parameterComponent.repaint();
                }
            });
        }
        parameterComponent.setMaximumSize(parameterComponent.getPreferredSize());
        parameterComponent.setMinimumSize(parameterComponent.getPreferredSize());
        return parameterComponent;
    }

    protected JPanel createParamPanel(ProcessorModel processorModel) {
        ArrayList<ParamInfo> paramList = processorModel.getProgramParamList();
        JPanel paramPanel = new JPanel();
        paramPanel.setName("param panel");
        JPanel textFieldPanel = new JPanel();
        textFieldPanel.setName("text field panel");
        JPanel booleanParamPanel = new JPanel();
        booleanParamPanel.setName("boolean field panel");
        JPanel fileParamPanel = new JPanel();
        fileParamPanel.setName("file parameter panel");

        TableLayout booelanParamLayout = new TableLayout(3);
        booleanParamPanel.setLayout(booelanParamLayout);

        TableLayout fileParamLayout = new TableLayout(1);
        fileParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        fileParamPanel.setLayout(fileParamLayout);

        int numberOfOptionsPerLine = paramList.size() % 4 < paramList.size() % 5 ? 4 : 5;
        TableLayout textFieldPanelLayout = new TableLayout(numberOfOptionsPerLine);
        textFieldPanel.setLayout(textFieldPanelLayout);

        Iterator itr = paramList.iterator();
        while (itr.hasNext()) {
            final ParamInfo pi = (ParamInfo) itr.next();
            if (!(pi.getName().equals(processorModel.getPrimaryInputFileOptionName()) ||
                    pi.getName().equals(processorModel.getPrimaryOutputFileOptionName()) ||
                    pi.getName().equals(L2genData.GEOFILE))) {

                SeadasLogger.getLogger().fine(pi.getName());
                if (pi.hasValidValueInfos()) {

                    textFieldPanel.add(makeComboBoxOptionPanel(pi));

                } else {
                    switch (pi.getType()) {
                        case BOOLEAN:
                            booleanParamPanel.add(makeBooleanOptionField(pi));
                            break;
                        case IFILE:
                            fileParamPanel.add(createIOFileOptionField(pi));
                            break;
                        case OFILE:
                            fileParamPanel.add(createIOFileOptionField(pi));
                            break;
                        case STRING:
                            textFieldPanel.add(makeOptionField(pi));
                            break;
                        case INT:
                            textFieldPanel.add(makeOptionField(pi));
                            break;
                        case FLOAT:
                            textFieldPanel.add(makeOptionField(pi));
                            break;
                    }
                    //paramPanel.add(makeOptionField(pi));
                }
            }
        }

        TableLayout paramLayout = new TableLayout(1);

        paramPanel.setLayout(paramLayout);
        paramPanel.add(fileParamPanel);
        paramPanel.add(textFieldPanel);
        paramPanel.add(booleanParamPanel);

        return paramPanel;
    }

    protected JPanel makeOptionField(final ParamInfo pi) {

        final String optionName = pi.getName();
        final JPanel optionPanel = new JPanel();
        optionPanel.setName(optionName );
        TableLayout fieldLayout = new TableLayout(1);
        fieldLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        optionPanel.setLayout(fieldLayout);
        optionPanel.setName(optionName);
        optionPanel.add(new JLabel(ParamUtils.removePreceedingDashes(optionName)));


        if (pi.getValue() == null || pi.getValue().length() == 0) {
            if (pi.getDefaultValue() != null) {
                processorModel.updateParamInfo(pi, pi.getDefaultValue());
            }
        }

        final PropertyContainer vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, pi.getValue()));
        vc.getDescriptor(optionName).setDisplayName(optionName);
        final BindingContext ctx = new BindingContext(vc);
        final JTextField field = new JTextField();
        field.setColumns(8);
        field.setPreferredSize(field.getPreferredSize());
        field.setMaximumSize(field.getPreferredSize());
        field.setMinimumSize(field.getPreferredSize());
        field.setName(pi.getName());

        if (pi.getDescription() != null) {
            field.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        ctx.bind(optionName, field);

        ctx.addPropertyChangeListener(optionName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                processorModel.updateParamInfo(pi, field.getText());
            }
        });
        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                field.setText(pi.getValue());
            }
        });
        optionPanel.add(field);

        return optionPanel;
    }

    private JPanel makeBooleanOptionField(final ParamInfo pi) {

        final String optionName = pi.getName();
        final boolean optionValue = pi.getValue().equals( "true" ) || pi.getValue().equals( "1") ? true : false;

        final JPanel optionPanel = new JPanel();
        optionPanel.setName( optionName );
        TableLayout booleanLayout = new TableLayout(1);
        //booleanLayout.setTableFill(TableLayout.Fill.HORIZONTAL);

        optionPanel.setLayout(booleanLayout);
        optionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        optionPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        optionPanel.add(new JLabel(emptySpace + ParamUtils.removePreceedingDashes(optionName) + emptySpace));


        final PropertySet vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, optionValue));
        vc.getDescriptor(optionName).setDisplayName(optionName);

        final ValueRange valueRange = new ValueRange(0, 1);


        vc.getDescriptor(optionName).setValueRange(valueRange);

        final BindingContext ctx = new BindingContext(vc);
        final JCheckBox field = new JCheckBox();
        field.setHorizontalAlignment(JFormattedTextField.LEFT);
        field.setName(pi.getName());
        if (pi.getDescription() != null) {
            field.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        SeadasLogger.getLogger().finest(optionName + "  " + pi.getValue());
        ctx.bind(optionName, field);

        ctx.addPropertyChangeListener(optionName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {

                processorModel.updateParamInfo(pi, (new Boolean(field.isSelected())).toString());
                SeadasLogger.getLogger().info((new Boolean(field.isSelected())).toString() + "  " + field.getText());

            }
        });

        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                field.setSelected(pi.getValue().equals("true") || pi.getValue().equals("1") ? true : false);
            }
        });

        optionPanel.add(field);

        return optionPanel;

    }

    private JPanel makeComboBoxOptionPanel(final ParamInfo pi) {
        final JPanel singlePanel = new JPanel();

        TableLayout comboParamLayout = new TableLayout(1);
        comboParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        singlePanel.setLayout(comboParamLayout);

        final JLabel optionNameLabel = new JLabel(ParamUtils.removePreceedingDashes(pi.getName()));

        singlePanel.add(optionNameLabel);


        String optionDefaultValue = pi.getValue();


        final ArrayList<ParamValidValueInfo> validValues = pi.getValidValueInfos();
        String[] values = new String[validValues.size()];
        ArrayList<String> toolTips = new ArrayList<String>();

        Iterator itr = validValues.iterator();
        int i = 0;
        ParamValidValueInfo paramValidValueInfo;
        while (itr.hasNext()) {
            paramValidValueInfo = (ParamValidValueInfo) itr.next();
            values[i] = paramValidValueInfo.getValue();
            toolTips.add(paramValidValueInfo.getDescription());
            i++;
        }

        final JComboBox inputList = new JComboBox(values);
        ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
        inputList.setRenderer(renderer);
        renderer.setTooltips(toolTips);
        inputList.setEditable(true);
        inputList.setName(pi.getName());
        inputList.setPreferredSize(new Dimension(inputList.getPreferredSize().width,
                inputList.getPreferredSize().height));
        if (pi.getDescription() != null) {
            inputList.setToolTipText(pi.getDescription());
        }
        int defaultValuePosition = validValues.indexOf(optionDefaultValue);

        if (defaultValuePosition != -1) {
            inputList.setSelectedIndex(defaultValuePosition);
        }

        String optionName = pi.getName();


        final PropertyContainer vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, pi.getValue()));
        vc.getDescriptor(optionName).setDisplayName(optionName);

        final BindingContext ctx = new BindingContext(vc);

        ctx.bind(optionName, inputList);

        ctx.addPropertyChangeListener(optionName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {

                String newValue = (String) inputList.getSelectedItem();
                processorModel.updateParamInfo(pi, newValue);
            }
        });

        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                int currentChoicePosition = validValues.indexOf(pi.getValue());
                if (currentChoicePosition != -1) {
                    inputList.setSelectedIndex(currentChoicePosition);
                }
            }
        });
        singlePanel.add(inputList);
        return singlePanel;
    }

    private JPanel createIOFileOptionField(final ParamInfo pi) {

        final FileSelector ioFileSelector = new FileSelector(VisatApp.getApp(), FileSelector.Type.OFILE, ParamUtils.removePreceedingDashes(pi.getName()));
        ioFileSelector.getFileTextField().setColumns(40);
        ioFileSelector.addPropertyChangeListener(ioFileSelector.getPropertyName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                String iofile;
                if (ioFileSelector.getFileName() != null) {
                    iofile = ioFileSelector.getFileName();
                    processorModel.updateParamInfo(pi, iofile);
                }
            }
        });

        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                ioFileSelector.setFilename(pi.getValue());
            }
        });
        return ioFileSelector.getjPanel();
    }

    private class ComboboxToolTipRenderer extends DefaultListCellRenderer {
        ArrayList tooltips;

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                            int index, boolean isSelected, boolean cellHasFocus) {

            JComponent comp = (JComponent) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

            if (-1 < index && null != value && null != tooltips) {
                        list.setToolTipText((String)tooltips.get(index));
                    }
            return comp;
        }

        public void setTooltips(ArrayList tooltips) {
            this.tooltips = tooltips;
        }
    }

}
