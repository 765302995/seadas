package gov.nasa.gsfc.seadas.contour.ui;

import gov.nasa.gsfc.seadas.contour.data.ContourData;
import gov.nasa.gsfc.seadas.contour.data.ContourInterval;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FilterBand;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.imgfilter.CreateFilteredBandAction;
import org.esa.beam.visat.actions.imgfilter.model.Filter;

import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 9/9/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContourDialog extends JDialog {

    public static final String TITLE = "Create Contour Lines"; /*I18N*/
    static final String NEW_BAND_SELECTED_PROPERTY = "newBandSelected";
    static final String DELETE_BUTTON_PRESSED_PROPERTY = "deleteButtonPressed";
    private ContourData contourData;
    private Component helpButton = null;
    private HelpBroker helpBroker = null;

    private final static String HELP_ID = "contourLines";
    private final static String HELP_ICON = "icons/Help24.gif";

    private Product product;

    Band selectedBand;
    Band selectedFilteredBand;
    Band selectedUnfilteredBand;
    int numberOfLevels;

    JComboBox bandComboBox;
    ArrayList<ContourData> contours;
    ArrayList<String> activeBands;

    private SwingPropertyChangeSupport propertyChangeSupport;

    JPanel contourPanel;
    private boolean contourCanceled;
    private String filteredBandName;
    boolean filterBand;

    private double noDataValue;
    RasterDataNode raster;

    private double NULL_DOUBLE = -1.0;
    private double ptsToPixelsMultiplier = NULL_DOUBLE;

    JCheckBox filtered = new JCheckBox("", true);


    public ContourDialog(Product product, ArrayList<String> activeBands) {
        super(VisatApp.getApp().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
        this.product = product;

        initHelpBroker();

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        if (helpBroker != null) {
            helpButton = getHelpButton(HELP_ID);
        }


        if (VisatApp.getApp().getSelectedProductNode() != null && activeBands.contains(VisatApp.getApp().getSelectedProductNode().getName())) {
            selectedUnfilteredBand = product.getBand(VisatApp.getApp().getSelectedProductNode().getName());
            //selectedBand = product.getBand(VisatApp.getApp().getSelectedProductNode().getName());
            selectedBand = getDefaultFilterBand();
            raster = product.getRasterDataNode(selectedUnfilteredBand.getName());
        } else {
            selectedUnfilteredBand = product.getBand(activeBands.get(0));
            //selectedBand = product.getBand(activeBands.get(0));  //todo - match this with the selected productNode
            selectedBand = getDefaultFilterBand();
            raster = product.getRasterDataNode(selectedUnfilteredBand.getName());

        }
        contourData = new ContourData(selectedBand);
        this.activeBands = activeBands;
        numberOfLevels = 1;
        contours = new ArrayList<ContourData>();
        propertyChangeSupport.addPropertyChangeListener(NEW_BAND_SELECTED_PROPERTY, getBandPropertyListener());
        propertyChangeSupport.addPropertyChangeListener(DELETE_BUTTON_PRESSED_PROPERTY, getDeleteButtonPropertyListener());
        noDataValue = selectedBand.getNoDataValue();
        createContourUI();
        contourCanceled = true;
    }

    private PropertyChangeListener getBandPropertyListener() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                for (ContourData contourData1 : contours) {
                    contourData1.setBand(selectedBand);
                }
            }
        };
    }

    private PropertyChangeListener getDeleteButtonPropertyListener() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                Component[] components = contourPanel.getComponents();
                for (Component component : components) {
                    if (component instanceof JPanel) {

                        Component[] jPanelComponents = ((JPanel) component).getComponents();
                        for (Component jPanelComponent : jPanelComponents) {
                            if (component instanceof JPanel && ((JPanel) jPanelComponent).getComponents().length == 0) {
                                ((JPanel) component).remove(jPanelComponent);
                            }
                        }
                    }
                    contourPanel.validate();
                    contourPanel.repaint();
                }
            }
        };
    }

    @Override
    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    @Override
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

    protected Component getHelpButton(String helpId) {
        if (helpId != null) {

            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(HELP_ICON),
                    false);

            HelpSet helpSet = helpBroker.getHelpSet();
            helpBroker.setCurrentID(helpId);

            if (helpButton != null) {
                helpButton.setToolTipText("Help");
                helpButton.setName("helpButton");
                helpBroker.enableHelpKey(helpButton, helpId, helpSet);
                helpBroker.enableHelpOnButton(helpButton, helpId, helpSet);
            }

            return helpButton;
        }

        return null;
    }


    private void initHelpBroker() {
        HelpSet helpSet = HelpSys.getHelpSet();
        if (helpSet != null) {
            helpBroker = helpSet.createHelpBroker();
            if (helpBroker instanceof DefaultHelpBroker) {
                DefaultHelpBroker defaultHelpBroker = (DefaultHelpBroker) helpBroker;
                defaultHelpBroker.setActivationWindow(this);
            }
        }
    }


    public final JPanel createContourUI() {


        final int rightInset = 5;

        contourPanel = new JPanel(new GridBagLayout());

        contourPanel.setBorder(BorderFactory.createTitledBorder(""));

        final JPanel contourContainerPanel = new JPanel(new GridBagLayout());

        final JPanel basicPanel = getContourPanel();

        contourContainerPanel.add(basicPanel,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        JButton addButton = new JButton("+");
        addButton.setPreferredSize(addButton.getPreferredSize());
        addButton.setMinimumSize(addButton.getPreferredSize());
        addButton.setMaximumSize(addButton.getPreferredSize());
        addButton.setName("addButton");

        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                JPanel addedPanel = getContourPanel();
                ((JButton) event.getSource()).getParent().add(addedPanel);
                JPanel c = (JPanel) ((JButton) event.getSource()).getParent();
                JPanel jPanel = (JPanel) c.getComponents()[0];
                int numPanels = jPanel.getComponents().length;
                jPanel.add(addedPanel,
                        new ExGridBagConstraints(0, numPanels, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
                repaint();
                pack();
            }
        });

        contourContainerPanel.addPropertyChangeListener("deleteButtonPressed", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                Component[] components = contourContainerPanel.getComponents();
                for (Component component : components) {
                    if (((JPanel) component).getComponents().length == 0) {
                        contourContainerPanel.remove(component);
                    }
                }
                contourContainerPanel.validate();
                contourContainerPanel.repaint();
            }
        });
        JPanel mainPanel = new JPanel(new GridBagLayout());


        contourPanel.add(contourContainerPanel,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        contourPanel.add(addButton,
                new ExGridBagConstraints(0, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        mainPanel.add(getBandPanel(),
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        mainPanel.add(contourPanel,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        mainPanel.add(getControllerPanel(),
                new ExGridBagConstraints(0, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        add(mainPanel);

        //this will set the "Create Contour Lines" button as a default button that listens to the Enter key
        mainPanel.getRootPane().setDefaultButton((JButton) ((JPanel) mainPanel.getComponent(2)).getComponent(2));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Contour Lines");
        //setTitle("Contour Lines for " + selectedBand.getName() );
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
        return mainPanel;
    }

    /**
     * By default a band should be filtered before running the contour algorithm on it.
     *
     * @return
     */
    private JPanel getBandPanel() {
        final int rightInset = 5;

        final JPanel bandPanel = new JPanel(new GridBagLayout());
        JLabel bandLabel = new JLabel("Product:");
        bandComboBox = new JComboBox(activeBands.toArray());
        bandComboBox.setSelectedItem(selectedBand.getName());
        bandComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String oldBandName = selectedBand.getName();
                selectedUnfilteredBand = product.getBand((String) bandComboBox.getSelectedItem());
                selectedBand = selectedUnfilteredBand;
                propertyChangeSupport.firePropertyChange(NEW_BAND_SELECTED_PROPERTY, oldBandName, selectedBand.getName());
                noDataValue = selectedBand.getGeophysicalNoDataValue();
            }
        });

        final JButton filterButton = new JButton("Choose Filter");
        final JCheckBox filtered = new JCheckBox("", true);
        final JTextArea filterMessage = new JTextArea("Using filtered band " + selectedFilteredBand.getName());
        filterMessage.setBackground(Color.lightGray);
        filterMessage.setEditable(false);

        filterButton.addActionListener(new ActionListener() {
            VisatApp visatApp = VisatApp.getApp();

            @Override
            public void actionPerformed(ActionEvent e) {
                Band currentFilteredBand = selectedFilteredBand;
                visatApp.getPreferences().setPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, false);
                CreateFilteredBandAction filteredBandAction = new CreateFilteredBandAction();
                VisatApp.getApp().setSelectedProductNode(selectedUnfilteredBand);
                if (selectedFilteredBand != null) {
                    product.getBandGroup().remove(product.getBand(selectedFilteredBand.getName()));
                }
                filteredBandAction.actionPerformed(getFilterCommandEvent(filteredBandAction, e));
                updateActiveBandList();
                visatApp.getPreferences().setPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true);
                if (filterBand) {
                    filterMessage.setText("Using filtered band " + selectedFilteredBand.getName());
                } else {
                    if (currentFilteredBand != null) {
                        product.getBandGroup().add(currentFilteredBand);
                    }
                }
                //((JPanel)bandPanel.getParent()).getRootPane().setDefaultButton((JButton)((JPanel)((JPanel)bandPanel.getParent()).getComponent(2)).getComponent(2));
            }
        });


        filtered.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (filtered.isSelected()) {
                    VisatApp.getApp().setSelectedProductNode(selectedUnfilteredBand);
                    selectedBand = getDefaultFilterBand();
                    selectedFilteredBand = selectedBand;
                    filterMessage.setText("Using filtered band " + selectedFilteredBand.getName());
                } else {
                    VisatApp.getApp().setSelectedProductNode(selectedUnfilteredBand);
                    selectedBand = selectedUnfilteredBand;
                    filterMessage.setText("Not filtered");
                    if (selectedFilteredBand != null) {
                        product.getBandGroup().remove(product.getBand(selectedFilteredBand.getName()));
                    }
                    selectedFilteredBand = null;
                }
            }
        });
        JLabel filler = new JLabel("     ");
        bandPanel.add(filler,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        bandPanel.add(bandLabel,
                new ExGridBagConstraints(1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        bandPanel.add(bandComboBox,
                new ExGridBagConstraints(2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        bandPanel.add(filterButton,
                new ExGridBagConstraints(3, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        bandPanel.add(filtered,
                new ExGridBagConstraints(4, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        bandPanel.add(filterMessage,
                new ExGridBagConstraints(5, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        return bandPanel;
    }

    private void updateActiveBandList() {

        Band[] bands = product.getBands();
        filterBand = false;
        for (Band band : bands) {
            //the image info of the filteredBand of the current band is null; this is to avoid selecting other filter bands and setting them to null
            if (band.getName().contains(selectedUnfilteredBand.getName()) && band.getName().length() > selectedUnfilteredBand.getName().length() && band.equals(bands[bands.length - 1])) {
                selectedBand = band;
                selectedFilteredBand = band;
                filteredBandName = band.getName();
                filterBand = true;
                noDataValue = selectedBand.getNoDataValue();
            }
        }

    }

    private JPanel getControllerPanel() {
        JPanel controllerPanel = new JPanel(new GridBagLayout());

        JButton createContourLines = new JButton("Create Contour Lines");
        createContourLines.setPreferredSize(createContourLines.getPreferredSize());
        createContourLines.setMinimumSize(createContourLines.getPreferredSize());
        createContourLines.setMaximumSize(createContourLines.getPreferredSize());
        createContourLines.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
//                if (contourData.getContourIntervals().size() == 0) {
//                    contourData.createContourLevels(getMinValue(), getMaxValue(), getNumberOfLevels(), logCheckBox.isSelected());
//                }
                contourCanceled = false;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(cancelButton.getPreferredSize());
        cancelButton.setMinimumSize(cancelButton.getPreferredSize());
        cancelButton.setMaximumSize(cancelButton.getPreferredSize());
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                contourCanceled = true;
                dispose();
            }
        });


        JLabel filler = new JLabel("                                        ");

        controllerPanel.add(filler,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        controllerPanel.add(cancelButton,
                new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        controllerPanel.add(createContourLines,
                new ExGridBagConstraints(3, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        controllerPanel.add(helpButton,
                new ExGridBagConstraints(5, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        return controllerPanel;
    }

    private JPanel getContourPanel() {
        ContourIntervalDialog contourIntervalDialog = new ContourIntervalDialog(selectedBand);
        contourIntervalDialog.addPropertyChangeListener(DELETE_BUTTON_PRESSED_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                propertyChangeSupport.firePropertyChange(DELETE_BUTTON_PRESSED_PROPERTY, true, false);
            }
        });
        contourIntervalDialog.addPropertyChangeListener(ContourIntervalDialog.CONTOUR_DATA_CHANGED_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                for (ContourData contourData : contours) {
                    contourData.createContourLevels();
                }
            }
        });
        contours.add(contourIntervalDialog.getContourData());
        return contourIntervalDialog.getBasicPanel();
    }


    public ContourData getContourData() {

        return getContourData(contours);
    }

    public ContourData getContourData(ArrayList<ContourData> contours) {
        ContourData mergedContourData = new ContourData(selectedBand);
        ArrayList<ContourInterval> contourIntervals = new ArrayList<ContourInterval>();
        for (ContourData contourData : contours) {
            contourIntervals.addAll(contourData.getContourIntervals());
        }
        mergedContourData.setContourIntervals(contourIntervals);
        mergedContourData.setFiltered(filterBand);
        return mergedContourData;
    }


    public boolean isContourCanceled() {
        return contourCanceled;
    }

    public void setContourCanceled(boolean contourCanceled) {
        this.contourCanceled = contourCanceled;
    }

    public String getFilteredBandName() {
        return filteredBandName;
    }

    public void setFilteredBandName(String filteredBandName) {
        this.filteredBandName = filteredBandName;
    }

    private CommandEvent getFilterCommandEvent(CreateFilteredBandAction command, ActionEvent actionEvent) {
        CommandEvent filterCommandEvent = new CommandEvent(command, actionEvent, null, null);
        command.setCommandID("createFilteredBand");
        command.setHelpId("createFilteredBand");
        command.setLongDescription("filter function in contour");
        return filterCommandEvent;
    }

    private FilterBand getDefaultFilterBand() {
        Filter defaultFilter = new Filter("Mean 2.5 Pixel Radius", "amc_2.5px", 5, 5, new double[]{
                0.172, 0.764, 1, 0.764, 0.172,
                0.764, 1, 1, 1, 0.764,
                1, 1, 1, 1, 1,
                0.764, 1, 1, 1, 0.764,
                0.172, 0.764, 1, 0.764, 0.172,
        }, 19.8);

        VisatApp.getApp().setSelectedProductNode(selectedUnfilteredBand);
        final FilterBand filteredBand = CreateFilteredBandAction.createFilterBand(defaultFilter, selectedUnfilteredBand.getName() + "_amc_2.5px", 1);
        filterBand = true;
        selectedFilteredBand = filteredBand;
        filteredBandName = filteredBand.getName();
        return filteredBand;
    }

    public double getNoDataValue() {
        return noDataValue;
    }

    public void setNoDataValue(double noDataValue) {
        this.noDataValue = noDataValue;
    }


    private double getPtsToPixelsMultiplier() {
        if (ptsToPixelsMultiplier == NULL_DOUBLE) {
            final double PTS_PER_INCH = 72.0;
            final double PAPER_HEIGHT = 11.0;
            final double PAPER_WIDTH = 8.5;

            double heightToWidthRatioPaper = (PAPER_HEIGHT) / (PAPER_WIDTH);
            double heightToWidthRatioRaster = raster.getRasterHeight() / raster.getRasterWidth();

            if (heightToWidthRatioRaster > heightToWidthRatioPaper) {
                // use height
                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterHeight() / (PAPER_HEIGHT));
            } else {
                // use width
                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterWidth() / (PAPER_WIDTH));
            }
        }

        return ptsToPixelsMultiplier;
    }
}
