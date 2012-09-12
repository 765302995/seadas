/*
Author: Danny Knowles
    Don Shea
*/

package gov.nasa.gsfc.seadas.processing.processor;

import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import gov.nasa.gsfc.seadas.processing.core.MultiParamList;
import gov.nasa.gsfc.seadas.processing.core.ParamList;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.general.CloProgramUI;
import gov.nasa.gsfc.seadas.processing.general.GridBagConstraintsCustom;
import gov.nasa.gsfc.seadas.processing.general.SourceProductFileSelector;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;
import sun.beans.editors.StringEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;


public class SPForm extends JPanel implements CloProgramUI {

    /*
    SPForm
        tabbedPane
            mainPanel
                primaryIOPanel
                    sourceProductFileSelector (ifile)
                parfilePanel
                    importPanel
                        importParfileButton
                        retainParfileCheckbox
                    exportParfileButton
                    parfileTextArea
            chainPanel
                nameLabel
                keepLabel
                paramsLabel
                configLabel
                progRowPanel


     */

    private AppContext appContext;

    private final JTabbedPane tabbedPane;

    private JPanel mainPanel;
    private JPanel primaryIOPanel;
    private SourceProductFileSelector sourceProductFileSelector;
    private JPanel parfilePanel;
    private JPanel importPanel;
    private JButton importParfileButton;
    private JCheckBox retainIFileCheckbox;
    private JButton exportParfileButton;
    private JTextArea parfileTextArea;

    private JPanel chainPanel;
    private JLabel nameLabel;
    private JLabel keepLabel;
    private JLabel paramsLabel;
    private JLabel configLabel;
    private JPanel spacer;

    private ArrayList<SPRow> rows;

    SPForm(AppContext appContext, String xmlFileName) {
        this.appContext = appContext;

        // create main panel
        sourceProductFileSelector = new SourceProductFileSelector(VisatApp.getApp(), "ifile");
        sourceProductFileSelector.initProducts();
        //sourceProductFileSelector.setProductNameLabel(new JLabel("ifile"));
        sourceProductFileSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "123456789 123456789 123456789 123456789 123456789 ");
        sourceProductFileSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
                handleIFileChanged();
            }

            @Override
            public void selectionContextChanged(SelectionChangeEvent selectionChangeEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        primaryIOPanel = new JPanel(new GridBagLayout());
        primaryIOPanel.setBorder(BorderFactory.createTitledBorder("Primary I/O Files"));
        primaryIOPanel.add(sourceProductFileSelector.createDefaultPanel(),
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

        importParfileButton = new JButton("Import Parfile");
        retainIFileCheckbox = new JCheckBox("Retain Selected IFILE");

        importPanel = new JPanel(new GridBagLayout());
        importPanel.setBorder(BorderFactory.createEtchedBorder());
        importPanel.add(importParfileButton,
                new GridBagConstraintsCustom(0, 0, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        importPanel.add(retainIFileCheckbox,
                new GridBagConstraintsCustom(1, 0, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

        exportParfileButton = new JButton("Export Parfile");
        parfileTextArea = new JTextArea();
        parfileTextArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void focusLost(FocusEvent e) {
                handleParamStringChange();
            }
        });

        parfilePanel = new JPanel(new GridBagLayout());
        parfilePanel.setBorder(BorderFactory.createTitledBorder("Parfile"));
        parfilePanel.add(importPanel,
                new GridBagConstraintsCustom(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        parfilePanel.add(exportParfileButton,
                new GridBagConstraintsCustom(1, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        parfilePanel.add(parfileTextArea,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 2));

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.add(primaryIOPanel,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        mainPanel.add(parfilePanel,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH));

        // create chain panel
        nameLabel = new JLabel("Program");
        Font font = nameLabel.getFont().deriveFont(Font.BOLD);
        nameLabel.setFont(font);
        keepLabel = new JLabel("Keep");
        keepLabel.setFont(font);
        paramsLabel = new JLabel("Params");
        paramsLabel.setFont(font);
        configLabel = new JLabel("Config");
        configLabel.setFont(font);
        spacer = new JPanel();

        chainPanel = new JPanel(new GridBagLayout());
        chainPanel.add(nameLabel,
                new GridBagConstraintsCustom(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 4));
        chainPanel.add(keepLabel,
                new GridBagConstraintsCustom(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 4));
        chainPanel.add(paramsLabel,
                new GridBagConstraintsCustom(2, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 4));
        chainPanel.add(configLabel,
                new GridBagConstraintsCustom(3, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 4));
        createRows();
        int rowNum = 1;
        for (SPRow row : rows) {
            row.attachComponents(chainPanel, rowNum);
            rowNum++;
        }
        chainPanel.add(spacer,
                new GridBagConstraintsCustom(0, rowNum, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL));


        tabbedPane = new JTabbedPane();
        tabbedPane.add("Main", mainPanel);
        tabbedPane.add("Program Chain", chainPanel);

        // add the tabbed pane
        setLayout(new GridBagLayout());
        add(tabbedPane, new GridBagConstraintsCustom(0, 0, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH));

    }

    void createRows() {
        String[] rowNames = {
                "main",
                "modis_L1A.py",
                "modis_GEO.py",
                "modis_L1B.py",
                "l1bgen",
                "l1brsgen",
                "l1mapgen",
                "l2gen",
                "l2brsgen",
                "l2mapgen",
                "l2bin",
                "l3bin",
                "smigen",
        };
        rows = new ArrayList<SPRow>();

        for (String name : rowNames) {
            SPRow row = new SPRow(name, this);
            row.addPropertyChangeListener(SPRow.PARAM_STRING_EVENT, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    updateParamString();
                }
            });
            rows.add(row);

        }
    }

    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public JPanel getParamPanel() {
        return this;
    }

    public ParamList getParamList() {
        MultiParamList paramList = new MultiParamList();
        for (SPRow row : rows) {
            paramList.addParamList(row.getName(), row.getParamList());
        }
        return paramList;
    }

    @Override
    public ProcessorModel getProcessorModel() {
        ProcessorModel processorModel = new ProcessorModel("seadas_processor.py");
        processorModel.setParamList(getParamList());
        return processorModel;
    }

    @Override
    public Product getSelectedSourceProduct() {
        if (getSourceProductFileSelector() != null) {
            return getSourceProductFileSelector().getSelectedProduct();
        }
        return null;
    }

    @Override
    public boolean isOpenOutputInApp() {
        return false;
    }

    public SourceProductFileSelector getSourceProductFileSelector() {
        return sourceProductFileSelector;
    }

    public void prepareShow() {
        if (getSourceProductFileSelector() != null) {
            getSourceProductFileSelector().initProducts();
        }
    }

    public void prepareHide() {
        if (getSourceProductFileSelector() != null) {
            getSourceProductFileSelector().releaseProducts();
        }
    }

    private SPRow getRow(String name) {
        for (SPRow row : rows) {
            if (row.getName().equals(name)) {
                return row;
            }
        }
        return null;
    }

    public String getParamString() {
        return getParamList().getParamString("\n");
    }

    public void setParamString(String str) {
        String[] lines = str.split("\n");

        String sectionName = "main";
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.length() > 0 && line.charAt(0) != '#') {
                if (line.charAt(0) == '[' && line.contains("]")) {
                    if (sb.length() > 0) {
                        SPRow row = getRow(sectionName);
                        if (row != null) {
                            row.setParamString(sb.toString());
                        }
                        sb.setLength(0);
                    }
                    line = line.substring(1).trim();
                    String[] words = line.split("\\s+", 2);
                    sectionName = words[0];
                    int i = sectionName.indexOf(']');
                    if (i != -1) {
                        sectionName = sectionName.substring(0, i).trim();
                    }
                } else {
                    sb.append(line).append("\n");
                }
            }

            if (sb.length() > 0) {
                SPRow row = getRow(sectionName);
                if (row != null) {
                    row.setParamString(sb.toString());
                }
            }

        }
        parfileTextArea.setText(getParamString());
    }

    private void updateParamString() {
        parfileTextArea.setText(getParamString());
    }

    private void handleParamStringChange() {
        String str = parfileTextArea.getText();
        setParamString(str);
    }

    private void handleIFileChanged() {
        String ifileName = sourceProductFileSelector.getSelectedProduct().getFileLocation().getAbsolutePath();
        getRow("main").setParamValue("ifile", ifileName);
        updateParamString();
    }

}