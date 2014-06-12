package gov.nasa.gsfc.seadas.contour.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.combobox.ColorComboBox;
import gov.nasa.gsfc.seadas.contour.util.CommonUtilities;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.ColorComboBoxAdapter;

import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 6/4/14
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContourIntervalDialog extends JDialog {
    private static final String DELETE_BUTTON_IMAGE_FILE_NAME = "delete_button.png";
    static final String CONTOUR_DATA_CHANGED_PROPERTY = "contourDataChanged";
    Double minValue;
    Double maxValue;
    int numberOfLevels;

    //UI components
    JTextField minValueField, maxValueField, numLevelsField;
    JCheckBox logCheckBox;

    ContourData contourData;

    ContourIntervalDialog(Band selectedBand) {
        contourData = new ContourData(selectedBand);
        numberOfLevels = 1;
        contourData.setNumOfLevels(numberOfLevels);

        contourData.addPropertyChangeListener(ContourDialog.NEW_BAND_SELECTED_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                setBand(contourData.getBand());
                propertyChangeSupport.firePropertyChange(CONTOUR_DATA_CHANGED_PROPERTY, true, false);
            }
        });
        setMaxValue(new Double(CommonUtilities.round(selectedBand.getStx().getMax(), 3)));
        setMinValue(new Double(CommonUtilities.round(selectedBand.getStx().getMin(), 3)));
        contourData.createContourLevels();
    }

    SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    public void setBand(Band newBand){
        numberOfLevels = 1;
        setMaxValue(new Double(CommonUtilities.round(newBand.getStx().getMax(), 3)));
        setMinValue(new Double(CommonUtilities.round(newBand.getStx().getMin(), 3)));
        minValueField.setText(new Double(getMinValue()).toString());
        maxValueField.setText(new Double(getMaxValue()).toString());
    }

    protected JPanel getBasicPanel() {

        final int rightInset = 5;
        final JPanel contourPanel = new JPanel(new GridBagLayout());
        contourPanel.setBorder(BorderFactory.createTitledBorder(""));

        final DecimalFormat df = new DecimalFormat("##.###");
        minValueField = new JFormattedTextField(df);
        minValueField.setColumns(4);
        JLabel minValueLabel = new JLabel("Start Value:");


        maxValueField = new JFormattedTextField(df);
        maxValueField.setColumns(4);

        JLabel maxValueLabel = new JLabel("End Value:");

        numLevelsField = new JFormattedTextField(new DecimalFormat("##"));
        numLevelsField.setColumns(2);

        PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(Property.create("minValueField", minValue));
        propertyContainer.addProperty(Property.create("maxValueField", maxValue));
        propertyContainer.addProperty(Property.create("numLevelsField", numberOfLevels));

        final BindingContext bindingContext = new BindingContext(propertyContainer);
        final PropertyChangeListener pcl_min = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                minValue = (Double) bindingContext.getBinding("minValueField").getPropertyValue();
                contourData.setStartValue(minValue);
                propertyChangeSupport.firePropertyChange(CONTOUR_DATA_CHANGED_PROPERTY, true, false);
            }
        };
        final PropertyChangeListener pcl_max = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                maxValue = (Double) bindingContext.getBinding("maxValueField").getPropertyValue();
                contourData.setEndValue(maxValue);
                propertyChangeSupport.firePropertyChange(CONTOUR_DATA_CHANGED_PROPERTY, true, false);
            }
        };
        final PropertyChangeListener pcl_num = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                numberOfLevels = (Integer) bindingContext.getBinding("numLevelsField").getPropertyValue();
                contourData.setNumOfLevels(numberOfLevels);
                propertyChangeSupport.firePropertyChange(CONTOUR_DATA_CHANGED_PROPERTY, true, false);
            }
        };
        JLabel numLevelsLabel = new JLabel("# of Levels:");

        Binding minValueBinding = bindingContext.bind("minValueField", minValueField);
        minValueBinding.addComponent(minValueLabel);
        bindingContext.addPropertyChangeListener("minValueField", pcl_min);

        Binding maxValueBinding = bindingContext.bind("maxValueField", maxValueField);
        maxValueBinding.addComponent(maxValueLabel);
        bindingContext.addPropertyChangeListener("maxValueField", pcl_max);

        Binding numLevelsBinding = bindingContext.bind("numLevelsField", numLevelsField);
        numLevelsBinding.addComponent(numLevelsLabel);
        bindingContext.addPropertyChangeListener("numLevelsField", pcl_num);

        logCheckBox = new JCheckBox();
        logCheckBox.setName("log checkbox");
        logCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
//                if (getNumberOfLevels() == contourData.getNumOfLevels()) {
//                    contourData.setKeepColors(true);
//                } else {
//                    contourData.setKeepColors(false);
//                }
                contourData.setLog(logCheckBox.isSelected());
                propertyChangeSupport.firePropertyChange(CONTOUR_DATA_CHANGED_PROPERTY, true, false);
                //contourData.createContourLevels(getMinValue(), getMaxValue(), getNumberOfLevels(), logCheckBox.isSelected());
            }
        });

        JLabel filler = new JLabel("      ");
        contourPanel.add(minValueLabel,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        contourPanel.add(minValueField,
                new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));

        contourPanel.add(filler,
                new ExGridBagConstraints(2, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        contourPanel.add(maxValueLabel,
                new ExGridBagConstraints(3, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        contourPanel.add(maxValueField,
                new ExGridBagConstraints(4, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));

        contourPanel.add(filler,
                new ExGridBagConstraints(5, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        contourPanel.add(numLevelsLabel,
                new ExGridBagConstraints(6, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        contourPanel.add(numLevelsField,
                new ExGridBagConstraints(7, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));

        contourPanel.add(filler,
                new ExGridBagConstraints(8, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        contourPanel.add(new JLabel("Log"),
                new ExGridBagConstraints(9, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        contourPanel.add(logCheckBox,
                new ExGridBagConstraints(10, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));

        JButton customize = new JButton("Customize");
        customize.setPreferredSize(customize.getPreferredSize());
        customize.setMinimumSize(customize.getPreferredSize());
        customize.setMaximumSize(customize.getPreferredSize());
        customize.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (contourData.getLevels().size() == 0 ||
                        minValue != contourData.getStartValue() ||
                        maxValue != contourData.getEndValue() ||
                        numberOfLevels != contourData.getNumOfLevels()) {
                    System.out.print("---change!---");

                    contourData.createContourLevels(getMinValue(), getMaxValue(), getNumberOfLevels(), logCheckBox.isSelected());
                }
                System.out.print("--- no change!---");
                customizeContourLevels(contourData);
            }
        });

        contourPanel.add(filler,
                new ExGridBagConstraints(11, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        contourPanel.add(customize,
                new ExGridBagConstraints(12, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        contourPanel.add(filler,
                new ExGridBagConstraints(13, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));

        JButton deleteButton = new JButton();
        ImageIcon imageIcon = new ImageIcon();

        java.net.URL imgURL = getClass().getResource(DELETE_BUTTON_IMAGE_FILE_NAME);
        if (imgURL != null) {
            imageIcon = new ImageIcon(imgURL, "Delete current row");
        } else {
            System.err.println("Couldn't find file: " + DELETE_BUTTON_IMAGE_FILE_NAME);
        }
        deleteButton.setIcon(imageIcon);
        deleteButton.setToolTipText("Delete current row");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                contourPanel.removeAll();
                contourPanel.validate();
                contourPanel.repaint();
                propertyChangeSupport.firePropertyChange("deleteButtonPressed", true, false);
                //propertyChangeSupport.firePropertyChange("deleteButtonPressed", true, false);
            }
        });

        contourPanel.add(deleteButton,
                new ExGridBagConstraints(14, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        return contourPanel;
    }

    private void setMinValue(Double minValue) {
        this.minValue = minValue;
        contourData.setStartValue(minValue);
    }

    private Double getMinValue() {
        return minValue;
    }

    private void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
        contourData.setEndValue(maxValue);
    }

    private Double getMaxValue() {
        return maxValue;
    }

    private void setNumberOfLevels(int numberOfLevels) {
        this.numberOfLevels = numberOfLevels;
        contourData.setNumOfLevels(numberOfLevels);
    }

    private int getNumberOfLevels() {
        return numberOfLevels;
    }

    private void customizeContourLevels(ContourData contourData) {
        final String contourNamePropertyName = "contourName";
        final String contourValuePropertyName = "contourValue";
        final String contourColorPropertyName = "contourColor";
        ArrayList<ContourInterval> contourIntervalsClone = contourData.cloneContourIntervals();
        JPanel customPanel = new JPanel();
        customPanel.setLayout(new BoxLayout(customPanel, BoxLayout.Y_AXIS));
        for (final ContourInterval interval : contourIntervalsClone) {
            JPanel contourLevelPanel = new JPanel(new TableLayout(7));
            JLabel contourNameLabel = new JLabel("Name: ");
            JLabel contourValueLabel = new JLabel("Value: ");
            JLabel contourColorLabel = new JLabel("Color: ");
            JTextField contourLevelName = new JTextField();
            contourLevelName.setColumns(20);
            contourLevelName.setText(interval.getContourLevelName());
            JTextField contourLevelValue = new JTextField();
            contourLevelValue.setColumns(5);
            contourLevelValue.setText(new Double(interval.getContourLevelValue()).toString());
            PropertyContainer propertyContainer = new PropertyContainer();
            propertyContainer.addProperty(Property.create(contourNamePropertyName, interval.getContourLevelName()));
            propertyContainer.addProperty(Property.create(contourValuePropertyName, interval.getContourLevelValue()));
            propertyContainer.addProperty(Property.create(contourColorPropertyName, interval.getLineColor()));
            final BindingContext bindingContext = new BindingContext(propertyContainer);
            final PropertyChangeListener pcl_name = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    interval.setContourLevelName((String) bindingContext.getBinding(contourNamePropertyName).getPropertyValue());
                }
            };
            final PropertyChangeListener pcl_value = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    interval.setContourLevelValue((Double) bindingContext.getBinding(contourValuePropertyName).getPropertyValue());
                }
            };
            final PropertyChangeListener pcl_color = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    interval.setLineColor((Color) bindingContext.getBinding(contourColorPropertyName).getPropertyValue());
                }
            };
            ColorComboBox contourLineColorComboBox = new ColorComboBox();
            contourLineColorComboBox.setColorValueVisible(false);
            contourLineColorComboBox.setAllowDefaultColor(true);
            Binding contourLineColorBinding = bindingContext.bind(contourColorPropertyName, new ColorComboBoxAdapter(contourLineColorComboBox));
            contourLineColorBinding.addComponent(contourColorLabel);
            bindingContext.addPropertyChangeListener(contourColorPropertyName, pcl_color);

            Binding contourNameBinding = bindingContext.bind(contourNamePropertyName, contourLevelName);
            contourNameBinding.addComponent(contourNameLabel);
            bindingContext.addPropertyChangeListener(contourNamePropertyName, pcl_name);

            Binding contourValueBinding = bindingContext.bind(contourValuePropertyName, contourLevelValue);
            contourValueBinding.addComponent(contourValueLabel);
            bindingContext.addPropertyChangeListener(contourValuePropertyName, pcl_value);

            contourLevelPanel.add(contourNameLabel);
            contourLevelPanel.add(contourLevelName);
            contourLevelPanel.add(contourValueLabel);
            contourLevelPanel.add(contourLevelValue);
            contourLevelPanel.add(contourColorLabel);
            contourLevelPanel.add(contourLineColorComboBox);
            customPanel.add(contourLevelPanel);
        }

        Object[] options = {"Save",
                "Cancel"};
        int n = JOptionPane.showOptionDialog(this, customPanel,
                "Customize Contour Levels",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[1]);
        if (n == JOptionPane.YES_OPTION) {
            contourData.setContourIntervals(contourIntervalsClone);
            minValueField.setText(new Double(contourIntervalsClone.get(0).getContourLevelValue()).toString());
            maxValueField.setText(new Double(contourIntervalsClone.get(contourIntervalsClone.size() - 1).getContourLevelValue()).toString());
            minValue = contourIntervalsClone.get(0).getContourLevelValue();
            maxValue = contourIntervalsClone.get(contourIntervalsClone.size() - 1).getContourLevelValue();
            contourData.setStartValue(minValue);
            contourData.setEndValue(maxValue);
        }
    }

    public ContourData getContourData() {
        return contourData;
    }

        public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
             propertyChangeSupport.addPropertyChangeListener(name, listener);
         }

         public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
             propertyChangeSupport.removePropertyChangeListener(name, listener);
         }

         public SwingPropertyChangeSupport getPropertyChangeSupport() {
             return propertyChangeSupport;
         }

         public void appendPropertyChangeSupport(SwingPropertyChangeSupport propertyChangeSupport) {
             PropertyChangeListener[] pr = propertyChangeSupport.getPropertyChangeListeners();
             for (int i = 0; i < pr.length; i++) {
                 this.propertyChangeSupport.addPropertyChangeListener(pr[i]);
             }
         }

         public void clearPropertyChangeSupport() {
             PropertyChangeListener[] pr = propertyChangeSupport.getPropertyChangeListeners();
             for (int i = 0; i < pr.length; i++) {
                 this.propertyChangeSupport.removePropertyChangeListener(pr[i]);
             }

         }
}
