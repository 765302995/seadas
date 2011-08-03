/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package gov.nasa.obpg.seadas.sandbox.l2gen;

import com.bc.ceres.binding.*;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.param.ParamParseException;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.DemSelector;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.crs.*;
import org.esa.beam.gpf.operators.reproject.CollocationCrsForm;
import org.esa.beam.util.ProductUtils;
import org.geotools.referencing.CRS;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Marco Zuehlke
 * @author Marco Paters
 * @since BEAM 4.7
 */
class L2genForm extends JTabbedPane {

    private static final String[] RESAMPLING_IDENTIFIER = {"Nearest", "Bilinear", "Bicubic"};

    private final boolean orthoMode;
    private final String targetProductSuffix = "L2";
    private final AppContext appContext;
    private final SourceProductSelector sourceProductSelector;
    private final TargetProductSelector targetProductSelector;
    private final Model reprojectionModel;
    private final PropertyContainer reprojectionContainer;
    private String geoFilenameSuffix = "GEO";

    private DemSelector demSelector;
    private CrsSelectionPanel crsSelectionPanel;

    private OutputGeometryFormModel outputGeometryModel;

    private JButton outputParamButton;
    private InfoForm infoForm;
    private CoordinateReferenceSystem crs;
    private CollocationCrsForm collocationCrsUI;
    private CustomCrsForm customCrsUI;

    private Document dom;
    public List productsList;

    L2genForm(TargetProductSelector targetProductSelector, boolean orthorectify, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        this.orthoMode = orthorectify;
        this.appContext = appContext;
        this.sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");
        if (orthoMode) {
            this.sourceProductSelector.setProductFilter(new OrthorectifyProductFilter());
        } else {
            this.sourceProductSelector.setProductFilter(new GeoCodingProductFilter());
        }
        this.reprojectionModel = new Model();
        this.reprojectionContainer = PropertyContainer.createObjectBacked(reprojectionModel);
        createUI();
    }

    void updateParameterMap(Map<String, Object> parameterMap) {
        parameterMap.clear();
        parameterMap.put("resamplingName", reprojectionModel.resamplingName);
        parameterMap.put("includeTiePointGrids", reprojectionModel.includeTiePointGrids);
        parameterMap.put("addDeltaBands", reprojectionModel.addDeltaBands);
        parameterMap.put("noDataValue", reprojectionModel.noDataValue);
        if (!collocationCrsUI.getRadioButton().isSelected()) {
            CoordinateReferenceSystem selectedCrs = getSelectedCrs();
            if (selectedCrs != null) {
                parameterMap.put("crs", selectedCrs.toWKT());
            }
        }
        if (orthoMode) {
            parameterMap.put("orthorectify", orthoMode);
            if (demSelector.isUsingExternalDem()) {
                parameterMap.put("elevationModelName", demSelector.getDemName());
            } else {
                parameterMap.put("elevationModelName", null);
            }
        }

        if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
            PropertySet container = outputGeometryModel.getPropertySet();
            parameterMap.put("referencePixelX", container.getValue("referencePixelX"));
            parameterMap.put("referencePixelY", container.getValue("referencePixelY"));
            parameterMap.put("easting", container.getValue("easting"));
            parameterMap.put("northing", container.getValue("northing"));
            parameterMap.put("orientation", container.getValue("orientation"));
            parameterMap.put("pixelSizeX", container.getValue("pixelSizeX"));
            parameterMap.put("pixelSizeY", container.getValue("pixelSizeY"));
            parameterMap.put("width", container.getValue("width"));
            parameterMap.put("height", container.getValue("height"));
        }
    }

    public void updateFormModel(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
        Property[] properties = reprojectionContainer.getProperties();
        for (Property property : properties) {
            String propertyName = property.getName();
            Object newValue = parameterMap.get(propertyName);
            if (newValue != null) {
                property.setValue(newValue);
            }
        }
        if (orthoMode) {
            Object elevationModelName = parameterMap.get("elevationModelName");
            if (elevationModelName instanceof String) {
                try {
                    demSelector.setDemName((String) elevationModelName);
                } catch (ParamValidateException e) {
                    throw new ValidationException(e.getMessage(), e);
                } catch (ParamParseException e) {
                    throw new ConversionException(e.getMessage(), e);
                }
            }
        }
        Object crsAsWKT = parameterMap.get("crs");
        if (crsAsWKT instanceof String) {
            try {
                CoordinateReferenceSystem crs = null;
                crs = CRS.parseWKT((String) crsAsWKT);
                if (crs instanceof ProjectedCRS) {
                    ProjectedCRS projectedCRS = (ProjectedCRS) crs;
                    Projection conversionFromBase = projectedCRS.getConversionFromBase();
                    OperationMethod operationMethod = conversionFromBase.getMethod();
                    ParameterValueGroup parameterValues = conversionFromBase.getParameterValues();
                    GeodeticDatum geodeticDatum = projectedCRS.getDatum();
                    customCrsUI.setCustom(geodeticDatum, operationMethod, parameterValues);
                } else {
                    throw new ConversionException("Failed to convert CRS from WKT.");
                }
            } catch (FactoryException e) {
                throw new ConversionException("Failed to convert CRS from WKT.", e);
            }

        }
        if (parameterMap.containsKey("referencePixelX")) {
            PropertyContainer propertySet = PropertyContainer.createMapBacked(parameterMap);
            outputGeometryModel = new OutputGeometryFormModel(propertySet);
            reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION, false);
        } else {
            outputGeometryModel = null;
            reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION, true);
        }
        updateCRS();
    }

    Map<String, Product> getProductMap() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", getSourceProduct());
        if (collocationCrsUI.getRadioButton().isSelected()) {
            productMap.put("collocateWith", collocationCrsUI.getCollocationProduct());
        }
        return productMap;
    }

    Product getSourceProduct() {
        return sourceProductSelector.getSelectedProduct();
    }

    CoordinateReferenceSystem getSelectedCrs() {
        return crs;
    }

    void prepareShow() {
        sourceProductSelector.initProducts();
        crsSelectionPanel.prepareShow();
    }

    void prepareHide() {
        sourceProductSelector.releaseProducts();
        crsSelectionPanel.prepareHide();
        if (outputGeometryModel != null) {
            outputGeometryModel.setSourceProduct(null);
        }
    }

    String getExternalDemName() {
        if (orthoMode && demSelector.isUsingExternalDem()) {
            return demSelector.getDemName();
        }
        return null;
    }

    private void createUI() {
        addTab("I/O Parameters", createIOPanel());

        // todo delete this call to createParametersPanel()
        // this is stupid but for right now in order to delete the tab as copied from the Reprojection tabs we must call this function
        JPanel junk = createParametersPanel();
        // addTab("Reprojection Parameters", createParametersPanel());
        addTab("Processing Parameters", createParfileTabPanel());
        addTab("Sub Sample", createSubsampleTabPanel());
        addTab("Product Selector", createProductSelectorPanel());

    }

    private JPanel createSubsampleTabPanel() {

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Lat-Lon", createLatlonTabPanel());
        tabbedPane.addTab("Pix-Line", createPixlineTabPanel());

        // Declare mainPanel and set it's attributes
        final JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createTitledBorder(""));
        mainPanel.setLayout(new GridBagLayout());


        // Add Swing controls to mainPanel grid cells
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            mainPanel.add(tabbedPane, c);
        }


        final JPanel finalMainPanel;
        finalMainPanel = addPaddedWrapperPanel(mainPanel, 6);

        return finalMainPanel;
    }


    private JPanel createPixlineTabPanel() {

        // Define all Swing controls used on this tab page
        final JTextField spixTextfield = new JTextField(5);
        final JTextField epixTextfield = new JTextField(5);
        final JTextField dpixTextfield = new JTextField(5);
        final JTextField slineTextfield = new JTextField(5);
        final JTextField elineTextfield = new JTextField(5);
        final JTextField dlineTextfield = new JTextField(5);

        final JLabel spixLabel = new JLabel("start pix");
        final JLabel epixLabel = new JLabel("end pix");
        final JLabel dpixLabel = new JLabel("delta pix");
        final JLabel slineLabel = new JLabel("start line");
        final JLabel elineLabel = new JLabel("end line");
        final JLabel dlineLabel = new JLabel("delta line");


        // Declare mainPanel and set it's attributes
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());


        final JPanel innerPanel1 = new JPanel();
        innerPanel1.setBorder(BorderFactory.createTitledBorder("Pixels"));
        innerPanel1.setLayout(new GridBagLayout());


        // Add Swing controls to mainPanel grid cells
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            innerPanel1.add(spixLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            innerPanel1.add(spixTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.EAST;
            innerPanel1.add(epixLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            innerPanel1.add(epixTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.anchor = GridBagConstraints.EAST;
            innerPanel1.add(dpixLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            innerPanel1.add(dpixTextfield, c);
        }


        final JPanel innerPanel2 = new JPanel();
        innerPanel2.setBorder(BorderFactory.createTitledBorder("Lines"));
        innerPanel2.setLayout(new GridBagLayout());

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            innerPanel2.add(slineLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            innerPanel2.add(slineTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.EAST;
            innerPanel2.add(elineLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            innerPanel2.add(elineTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.anchor = GridBagConstraints.EAST;
            innerPanel2.add(dlineLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            innerPanel2.add(dlineTextfield, c);
        }


        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            mainPanel.add(innerPanel1, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            mainPanel.add(innerPanel2, c);
        }


        final JPanel finalMainPanel = new JPanel();
        finalMainPanel.setLayout(new GridBagLayout());

        {
            final GridBagConstraints c;
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(3, 3, 3, 3);
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            c.weighty = 1;

            finalMainPanel.add(mainPanel, c);
        }

        return finalMainPanel;
    }


    private JPanel createLatlonTabPanel() {

        // Define all Swing controls used on this tab page
        final JTextField northTextfield = new JTextField(5);
        final JTextField southTextfield = new JTextField(5);
        final JTextField westTextfield = new JTextField(5);
        final JTextField eastTextfield = new JTextField(5);

        final JLabel northLabel = new JLabel("N");
        final JLabel southLabel = new JLabel("S");
        final JLabel westLabel = new JLabel("W");
        final JLabel eastLabel = new JLabel("E");


        // Declare mainPanel and set it's attributes
        final JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createTitledBorder("Coordinates"));
        mainPanel.setLayout(new GridBagLayout());


        // Add Swing controls to mainPanel grid cells
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 1;
            c.anchor = GridBagConstraints.NORTH;
            mainPanel.add(northTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 3;
            c.anchor = GridBagConstraints.SOUTH;
            mainPanel.add(southTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 2;
            c.anchor = GridBagConstraints.EAST;
            mainPanel.add(eastTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            c.anchor = GridBagConstraints.WEST;
            mainPanel.add(westTextfield, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.anchor = GridBagConstraints.SOUTH;
            mainPanel.add(northLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 4;
            c.anchor = GridBagConstraints.NORTH;
            mainPanel.add(southLabel, c);
        }

        {


            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 4;
            c.gridy = 2;
            c.anchor = GridBagConstraints.WEST;
            mainPanel.add(eastLabel, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.anchor = GridBagConstraints.EAST;
            mainPanel.add(westLabel, c);
        }

        final JPanel finalMainPanel = new JPanel();
        finalMainPanel.setLayout(new GridBagLayout());

        {
            final GridBagConstraints c;
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(3, 3, 3, 3);
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            c.weighty = 1;

            finalMainPanel.add(mainPanel, c);
        }

        return finalMainPanel;
    }


    private JPanel createParfileTabPanel() {

        // Define all Swing controls used on this tab page
        final JButton openButton = new JButton("Open");
        final JButton saveButton = new JButton("Save");
        final JTextArea textArea = new JTextArea();

        // Declare mainPanel and set it's attributes
        final JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createTitledBorder("Parfile"));
        mainPanel.setLayout(new GridBagLayout());

        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {

            @Override
            public void selectionChanged(SelectionChangeEvent event) {

                final Product sourceProduct = getSourceProduct();

                String myProduct = sourceProduct.getName();
                String myParfileLineIfile = "ifile=" + myProduct;

                textArea.setText(myParfileLineIfile);

//                sourceProductSelector.setSelectedProduct(sourceProduct);

                updateTargetProductName(sourceProduct);
/*                updateGeoFilename(sourceProduct);
                GeoPos centerGeoPos = null;
                if (sourceProduct != null) {
                    centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                }
                infoForm.setCenterPos(centerGeoPos);
                if (outputGeometryModel != null) {
                    outputGeometryModel.setSourceProduct(sourceProduct);
                }
                updateCRS(); */
            }
        });

        // Add openButton control to a mainPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            mainPanel.add(openButton, c);
        }

        // Add saveButton control to a mainPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            mainPanel.add(saveButton, c);
        }

        // Add textArea control to a mainPanel grid cell
        {
            JScrollPane scrollTextArea = new JScrollPane(textArea);

            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            mainPanel.add(scrollTextArea, c);
        }

        final JPanel finalMainPanel;
        finalMainPanel = addPaddedWrapperPanel(mainPanel, 3);

        return finalMainPanel;
    }


    private JPanel createProductSelectorPanel() {

        final JPanel wavelengthsPanel = new JPanel();
        final JPanel productWavelengthIndependentPanel = new JPanel();
        final JPanel productWavelengthDependentPanel = new JPanel();
        final JPanel selectedProductsPanel = new JPanel();

        ArrayList<ProductInfo> waveIndependentProductInfoArray;
        ArrayList<ProductInfo> waveDependentProductInfoArray;

        final String SEADAS_PRODUCTS_FILE = "/home/knowles/SeaDAS/seadas/seadas-sandbox/productList.xml";

        L2genXmlReader l2genXmlReader = new L2genXmlReader();

        l2genXmlReader.parseXmlFile(SEADAS_PRODUCTS_FILE);

        waveDependentProductInfoArray = l2genXmlReader.getWaveDependentProductInfoArray();
        waveIndependentProductInfoArray = l2genXmlReader.getWaveIndependentProductInfoArray();

        Collections.sort(waveIndependentProductInfoArray, ProductInfo.CASE_INSENSITIVE_ORDER);
        Collections.sort(waveDependentProductInfoArray, ProductInfo.CASE_INSENSITIVE_ORDER);


//        for (ProductInfo productInfo : waveDependentProductInfoArray) {
//            // productInfo.dump();
//           System.out.println(productInfo.toString());
//        }




        final JTextArea selectedProductsJTextArea = new JTextArea();

        selectedProductsJTextArea.setEditable(false);

        selectedProductsJTextArea.setLineWrap(true);

        selectedProductsJTextArea.setWrapStyleWord(true);

        selectedProductsJTextArea.setColumns(20);

        selectedProductsJTextArea.setRows(5);


        final JList waveIndependentJList = new JList();
        final JList waveDependentJList = new JList();

        final JLabel waveIndependentSelectedProductsJLabel = new JLabel();
        final JLabel waveDependentSelectedProductsJLabel = new JLabel();

        waveIndependentSelectedProductsJLabel.setText("test");
        waveDependentSelectedProductsJLabel.setText("test");


        final JLabel mySelectedProductsJLabel = new JLabel();


        mySelectedProductsJLabel.setText("test");
        createProductSelectorProductListPanel(productWavelengthIndependentPanel, waveIndependentProductInfoArray, "Products (Wavelength Independent)", waveIndependentJList);

        createProductSelectorWavelengthsPanel(wavelengthsPanel);

        createProductSelectorProductListPanel(productWavelengthDependentPanel, waveDependentProductInfoArray, "Products (Wavelength Dependent)", waveDependentJList);


        createSelectedProductsPanel(selectedProductsPanel, waveDependentJList, waveIndependentJList);


        // Declare mainPanel and set it's attributes
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new

                GridBagLayout()

        );


        // Add to mainPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.weightx = 0;
            c.weighty = 0;
            mainPanel.add(wavelengthsPanel, c);
        }


        // Add to mainPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTH;
            c.weightx = 0;
            c.weighty = 0;
            mainPanel.add(productWavelengthDependentPanel, c);
        }


        // Add to mainPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTH;
            c.weightx = 0;
            c.weighty = 0;
            mainPanel.add(productWavelengthIndependentPanel, c);
        }

        // Add to mainPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 3;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTH;
            c.weightx = 1;
            c.weighty = 1;
            mainPanel.add(selectedProductsPanel, c);
        }


        final JPanel finalMainPanel = new JPanel();
        finalMainPanel.setLayout(new

                GridBagLayout()

        );

        {
            final GridBagConstraints c;
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(3, 3, 3, 3);
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;

            finalMainPanel.add(mainPanel, c);
        }


        return finalMainPanel;
    }


    private void createProductSelectorProductListPanel(JPanel productPanel, ArrayList<ProductInfo> productInfoArrayList,
                                                       String myTitle, JList myJList) {


        // Create arrayList for all the algorithms

        ArrayList<AlgorithmInfo> myJListArrayList = new ArrayList<AlgorithmInfo>();

        for (ProductInfo currProductInfo : productInfoArrayList) {

            for (AlgorithmInfo currAlgorithmInfo : currProductInfo.getAlgorithmInfoArrayList()) {
                currAlgorithmInfo.setToStringShowProductName(true);
                //currAlgorithmInfo.setToStringShowParameterType(true);
                myJListArrayList.add(currAlgorithmInfo);
            }

        }

        // Store the arrayList into an array which can be fed into a JList control

        AlgorithmInfo[] myJListArray = new AlgorithmInfo[myJListArrayList.size()];
        myJListArrayList.toArray(myJListArray);

        // make and format the JList control
        //myJList = new JList();
        myJList.setListData(myJListArray);
        JScrollPane scrollPane = new JScrollPane(myJList);
        scrollPane.setMinimumSize(new Dimension(400, 100));

        scrollPane.setMaximumSize(new Dimension(400, 100));
        scrollPane.setPreferredSize(new Dimension(400, 100));

        productPanel.setBorder(BorderFactory.createTitledBorder(myTitle));
        productPanel.setLayout(new GridBagLayout());


        // Add to productPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            productPanel.add(scrollPane, c);
        }

    }


    private void createSelectedProductsPanel(JPanel myPanel, final JList waveDependentJList, final JList waveIndependentJList) {
        myPanel.setBorder(BorderFactory.createTitledBorder("Selected Products"));
        myPanel.setLayout(new GridBagLayout());

        final JTextArea selectedProductsJTextArea = new JTextArea();
        selectedProductsJTextArea.setEditable(false);
        selectedProductsJTextArea.setLineWrap(true);
        selectedProductsJTextArea.setWrapStyleWord(true);
        selectedProductsJTextArea.setColumns(20);
        selectedProductsJTextArea.setRows(5);

        final StringBuilder waveIndependentSelectedProductsString = new StringBuilder();
        final StringBuilder waveDependentSelectedProductsString = new StringBuilder();

        waveDependentJList.addListSelectionListener(new ListSelectionListener() {
            @Override

            public void valueChanged(ListSelectionEvent e) {
                //To change body of implemented methods use File | Settings | File Templates
                String what = "";

                Object values[] = waveDependentJList.getSelectedValues();

                for (int i = 0; i < values.length; i++) {
                    what += values[i].toString() + " ";
                }

                int myEnd =  waveDependentSelectedProductsString.length();
                waveDependentSelectedProductsString.delete(0,myEnd);
                waveDependentSelectedProductsString.append(what);

                String mySelectedProductsString = what + waveIndependentSelectedProductsString.toString();
                selectedProductsJTextArea.setText(mySelectedProductsString);


            }
        });


        waveIndependentJList.addListSelectionListener(new ListSelectionListener() {
            @Override

            public void valueChanged(ListSelectionEvent e) {
                //To change body of implemented methods use File | Settings | File Templates
                String what = "";

                Object values[] = waveIndependentJList.getSelectedValues();

                for (int i = 0; i < values.length; i++) {
                    what += " " + values[i].toString();
                }

                int myEnd =  waveIndependentSelectedProductsString.length();
                waveIndependentSelectedProductsString.delete(0,myEnd);
                waveIndependentSelectedProductsString.append(what);

                String mySelectedProductsString = waveDependentSelectedProductsString.toString() + what;

                selectedProductsJTextArea.setText(mySelectedProductsString);
            }
        });


        // Add openButton control to a mainPanel grid cell
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            myPanel.add(selectedProductsJTextArea, c);
        }
    }

    public Properties getEnvironment() throws java.io.IOException {
    Properties env = new Properties();
    env.load(Runtime.getRuntime().exec("env").getInputStream());
    return env;
    }



    private void createProductSelectorWavelengthsPanel(JPanel wavelengthsPanel) {
        final JLabel myLabel = new JLabel("wavelengths are here");
        wavelengthsPanel.setBorder(BorderFactory.createTitledBorder("Wavelengths"));
        wavelengthsPanel.setLayout(new GridBagLayout());

        String myEnvVar = System.getenv("HOME");

        System.out.println("HOME=" + myEnvVar);

/*
        final String TEMP_DATA_FILE = "/home/knowles/SeaDAS/seadas/seadas-sandbox/dataTest.txt";
        final ArrayList<String> myAsciiFileArrayList = myReadDataFile(TEMP_DATA_FILE);

        for (String myLine : myAsciiFileArrayList) {
            String splitLine[] = myLine.split("=");

            if (splitLine.length == 2) {
                System.out.println(splitLine[0] + " EQUALS " + splitLine[1] );
            }
            else
            {
                System.out.println("JUNK:" + myLine);
            }
        }
*/



        ArrayList<String> wavelengthsCheckboxArrayList = null;

/*
        for (int i=0; i < 5; i++) {
            StringBuilder myString = new StringBuilder("Hello");
            myString.append(i);

            if (myString.toString() != null)
            wavelengthsCheckboxArrayList.add("how");
        }
*/


        if (wavelengthsCheckboxArrayList != null) {
        for (int i=0; i < wavelengthsCheckboxArrayList.size(); i++) {
            StringBuilder myString = new StringBuilder("Hello");
            myString.append(i);

            JCheckBox tmpCheckbox = new JCheckBox(myString.toString());

    //        String myString = wavelengthsCheckboxArrayList.get(i);

      //      JCheckBox tmpCheckbox = new JCheckBox(myString);


            {
                final GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = i;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 1;
                wavelengthsPanel.add(tmpCheckbox, c);
            }

        }
        }


        // Add openButton control to a mainPanel grid cell
/*
        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            wavelengthsPanel.add(myLabel, c);
        }
*/
    }


    private JPanel addPaddedWrapperPanel(JPanel myMainPanel, int pad) {

        JPanel myWrapperPanel = new JPanel();

        myWrapperPanel.setLayout(new GridBagLayout());

        final GridBagConstraints c;
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(pad, pad, pad, pad);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        myWrapperPanel.add(myMainPanel, c);

        return myWrapperPanel;
    }


    private ArrayList<String> myReadDataFile(String fileName) {
                String lineData;
                ArrayList<String> fileContents = new ArrayList<String>();
                BufferedReader moFile=null;
                try {
                        moFile = new BufferedReader (new FileReader(new File( fileName)));
                        while ((lineData = moFile.readLine()) != null)
                        {

                                fileContents.add(lineData);
                        }
                } catch(IOException e) {
                       ;
                }finally {
                        try {
                                moFile.close();
                        }catch(Exception e) {
                                //Ignore
                        }
                }
                return fileContents;
        }

    private JPanel createIOPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(3, 3);

        final JPanel ioPanel = new JPanel(tableLayout);
        ioPanel.add(createSourceProductPanel());
//        ioPanel.add(createGeolocationProductPanel());
        ioPanel.add(targetProductSelector.createDefaultPanel());
        ioPanel.add(tableLayout.createVerticalSpacer());
        return ioPanel;
    }

    private JPanel createParametersPanel() {
        final JPanel parameterPanel = new JPanel();
        final TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        parameterPanel.setLayout(layout);
        customCrsUI = new CustomCrsForm(appContext);
        CrsForm predefinedCrsUI = new PredefinedCrsForm(appContext);
        collocationCrsUI = new CollocationCrsForm(appContext);
        CrsForm[] crsForms = new CrsForm[]{customCrsUI, predefinedCrsUI, collocationCrsUI};
        crsSelectionPanel = new CrsSelectionPanel(crsForms);
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product product = (Product) event.getSelection().getSelectedValue();
                crsSelectionPanel.setReferenceProduct(product);
            }
        });

        parameterPanel.add(crsSelectionPanel);
        if (orthoMode) {
            demSelector = new DemSelector();
            parameterPanel.add(demSelector);
        }
        parameterPanel.add(createOuputSettingsPanel());
        infoForm = new InfoForm();
        parameterPanel.add(infoForm.createUI());

        crsSelectionPanel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateCRS();
            }
        });
        updateCRS();
        return parameterPanel;
    }

    private void updateCRS() {
        final Product sourceProduct = getSourceProduct();
        try {
            if (sourceProduct != null) {
                crs = crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(sourceProduct));
                if (crs != null) {
                    infoForm.setCrsInfoText(crs.getName().getCode(), crs.toString());
                } else {
                    infoForm.setCrsErrorText("No valid 'Coordinate Reference System' selected.");
                }
            } else {
                infoForm.setCrsErrorText("No source product selected.");
                crs = null;
            }
        } catch (FactoryException e) {
            infoForm.setCrsErrorText(e.getMessage());
            crs = null;
        }
        if (outputGeometryModel != null) {
            outputGeometryModel.setTargetCrs(crs);
        }
        updateOutputParameterState();
    }

    private void updateProductSize() {
        int width = 0;
        int height = 0;
        final Product sourceProduct = getSourceProduct();
        if (sourceProduct != null && crs != null) {
            if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
                PropertySet container = outputGeometryModel.getPropertySet();
                width = (Integer) container.getValue("width");
                height = (Integer) container.getValue("height");
            } else {
                ImageGeometry iGeometry;
                final Product collocationProduct = collocationCrsUI.getCollocationProduct();
                if (collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                    iGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
                } else {
                    iGeometry = ImageGeometry.createTargetGeometry(sourceProduct, crs,
                            null, null, null, null,
                            null, null, null, null,
                            null);

                }
                Rectangle imageRect = iGeometry.getImageRect();
                width = imageRect.width;
                height = imageRect.height;
            }
        }
        infoForm.setWidth(width);
        infoForm.setHeight(height);
    }

    private class InfoForm {

        private JLabel widthLabel;
        private JLabel heightLabel;
        private JLabel centerLatLabel;
        private JLabel centerLonLabel;
        private JLabel crsLabel;
        private String wkt;
        private JButton wktButton;

        void setWidth(int width) {
            widthLabel.setText(Integer.toString(width));
        }

        void setHeight(int height) {
            heightLabel.setText(Integer.toString(height));
        }

        void setCenterPos(GeoPos geoPos) {
            if (geoPos != null) {
                centerLatLabel.setText(geoPos.getLatString());
                centerLonLabel.setText(geoPos.getLonString());
            } else {
                centerLatLabel.setText("");
                centerLonLabel.setText("");
            }
        }

        void setCrsErrorText(String infoText) {
            setCrsInfoText("<html><b>" + infoText + "</b>", null);
        }

        void setCrsInfoText(String infoText, String wkt) {
            this.wkt = wkt;
            crsLabel.setText(infoText);
            boolean hasWKT = (wkt != null);
            wktButton.setEnabled(hasWKT);
        }

        JPanel createUI() {
            widthLabel = new JLabel();
            heightLabel = new JLabel();
            centerLatLabel = new JLabel();
            centerLonLabel = new JLabel();
            crsLabel = new JLabel();

            final TableLayout tableLayout = new TableLayout(5);
            tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setColumnWeightX(0, 0.0);
            tableLayout.setColumnWeightX(1, 0.0);
            tableLayout.setColumnWeightX(2, 1.0);
            tableLayout.setColumnWeightX(3, 0.0);
            tableLayout.setColumnWeightX(4, 1.0);
            tableLayout.setCellColspan(2, 1, 3);
            tableLayout.setCellPadding(0, 3, new Insets(4, 24, 4, 20));
            tableLayout.setCellPadding(1, 3, new Insets(4, 24, 4, 20));


            final JPanel panel = new JPanel(tableLayout);
            panel.setBorder(BorderFactory.createTitledBorder("Output Information"));
            panel.add(new JLabel("Scene width:"));
            panel.add(widthLabel);
            panel.add(new JLabel("pixel"));
            panel.add(new JLabel("Center longitude:"));
            panel.add(centerLonLabel);

            panel.add(new JLabel("Scene height:"));
            panel.add(heightLabel);
            panel.add(new JLabel("pixel"));
            panel.add(new JLabel("Center latitude:"));
            panel.add(centerLatLabel);

            panel.add(new JLabel("CRS:"));
            panel.add(crsLabel);
            wktButton = new JButton("Show WKT");
            wktButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JTextArea wktArea = new JTextArea(30, 40);
                    wktArea.setEditable(false);
                    wktArea.setText(wkt);
                    final JScrollPane scrollPane = new JScrollPane(wktArea);
                    final ModalDialog dialog = new ModalDialog(appContext.getApplicationWindow(),
                            "Coordinate reference system as well known text",
                            scrollPane,
                            ModalDialog.ID_OK, null);
                    dialog.show();
                }
            });
            wktButton.setEnabled(false);
            panel.add(wktButton);
            return panel;
        }
    }

    private JPanel createOuputSettingsPanel() {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnFill(0, TableLayout.Fill.NONE);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setColumnPadding(0, new Insets(4, 4, 4, 20));
        tableLayout.setColumnWeightX(0, 0.0);
        tableLayout.setColumnWeightX(1, 0.0);
        tableLayout.setColumnWeightX(2, 1.0);
        tableLayout.setCellColspan(0, 1, 2);
        tableLayout.setCellPadding(1, 0, new Insets(4, 24, 4, 20));

        final JPanel outputSettingsPanel = new JPanel(tableLayout);
        outputSettingsPanel.setBorder(BorderFactory.createTitledBorder("Output Settings"));

        final BindingContext context = new BindingContext(reprojectionContainer);

        final JCheckBox preserveResolutionCheckBox = new JCheckBox("Preserve resolution");
        context.bind(Model.PRESERVE_RESOLUTION, preserveResolutionCheckBox);
        collocationCrsUI.getCrsUI().addPropertyChangeListener("collocate", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean collocate = (Boolean) evt.getNewValue();
                reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION,
                        collocate || reprojectionModel.preserveResolution);
                preserveResolutionCheckBox.setEnabled(!collocate);
            }
        });
        outputSettingsPanel.add(preserveResolutionCheckBox);

        JCheckBox includeTPcheck = new JCheckBox("Reproject tie-point grids", true);
        context.bind(Model.REPROJ_TIEPOINTS, includeTPcheck);
        outputSettingsPanel.add(includeTPcheck);

        outputParamButton = new JButton("Output Parameters...");
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution);
        outputParamButton.addActionListener(new OutputParamActionListener());
        outputSettingsPanel.add(outputParamButton);

        outputSettingsPanel.add(new JLabel("No-data value:"));
        final JTextField noDataField = new JTextField();

        outputSettingsPanel.add(noDataField);
        context.bind(Model.NO_DATA_VALUE, noDataField);

        JCheckBox addDeltaBandsChecker = new JCheckBox("Add delta lat/lon bands");
        outputSettingsPanel.add(addDeltaBandsChecker);
        context.bind(Model.ADD_DELTA_BANDS, addDeltaBandsChecker);

        outputSettingsPanel.add(new JLabel("Resampling method:"));
        JComboBox resampleComboBox = new JComboBox(RESAMPLING_IDENTIFIER);
        resampleComboBox.setPrototypeDisplayValue(RESAMPLING_IDENTIFIER[0]);
        context.bind(Model.RESAMPLING_NAME, resampleComboBox);
        outputSettingsPanel.add(resampleComboBox);

        reprojectionContainer.addPropertyChangeListener(Model.PRESERVE_RESOLUTION, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateOutputParameterState();
            }
        });

        return outputSettingsPanel;
    }

    private void updateOutputParameterState() {
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution && (crs != null));
        updateProductSize();
    }

    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();

        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product sourceProduct = getSourceProduct();
                updateTargetProductName(sourceProduct);
                updateGeoFilename(sourceProduct);
                GeoPos centerGeoPos = null;
                if (sourceProduct != null) {
                    centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                }
                infoForm.setCenterPos(centerGeoPos);
                if (outputGeometryModel != null) {
                    outputGeometryModel.setSourceProduct(sourceProduct);
                }
                updateCRS();
            }
        });
        return panel;
    }


    private JPanel createGeolocationProductPanel() {
        // final JPanel panel = sourceProductSelector.createDefaultPanel();
        final JPanel panel = createDefaultPanelNew();

/*
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product sourceProduct = getSourceProduct();
                updateTargetProductName(sourceProduct);
                GeoPos centerGeoPos = null;
                if (sourceProduct != null) {
                    centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                }
                infoForm.setCenterPos(centerGeoPos);
                if (outputGeometryModel != null) {
                    outputGeometryModel.setSourceProduct(sourceProduct);
                }
                updateCRS();
            }
        });
*/

        return panel;
    }


    public JPanel createDefaultPanelNew() {


        final JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createTitledBorder("Source Geolocation Product"));
        mainPanel.setLayout(new GridBagLayout());

        final JComboBox geolocationfileComboBox = new JComboBox();
        final JButton geolocationfileButton = new JButton("...");
        final JLabel name = new JLabel("Name:");


        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            mainPanel.add(name, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            mainPanel.add(geolocationfileComboBox, c);
        }

        {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            mainPanel.add(geolocationfileButton, c);
        }


        return mainPanel;
    }


    private void updateTargetProductName(Product selectedProduct) {
        String productName = "output." + targetProductSuffix;
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (selectedProduct != null) {
            int i = selectedProduct.getName().lastIndexOf('.');
            if (i != -1) {
                String baseName = selectedProduct.getName().substring(0, i);
                productName = baseName + "." + targetProductSuffix;
            } else {
                productName = selectedProduct.getName() + "." + targetProductSuffix;
            }
        }
        selectorModel.setProductName(productName);
    }


    private void updateGeoFilename(Product selectedProduct) {
        String productName = "output." + geoFilenameSuffix;
        if (selectedProduct != null) {
            int i = selectedProduct.getName().lastIndexOf('.');
            if (i != -1) {
                String baseName = selectedProduct.getName().substring(0, i);
                productName = baseName + "." + geoFilenameSuffix;
            } else {
                productName = selectedProduct.getName() + "." + geoFilenameSuffix;
            }
        }
        //selectorModel.setProductName(productName);
    }

    private class OutputParamActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                final Product sourceProduct = getSourceProduct();
                if (sourceProduct == null) {
                    showWarningMessage("Please select a product to reproject.\n");
                    return;
                }
                if (crs == null) {
                    showWarningMessage("Please specify a 'Coordinate Reference System' first.\n");
                    return;
                }
                OutputGeometryFormModel workCopy;
                if (outputGeometryModel != null) {
                    workCopy = new OutputGeometryFormModel(outputGeometryModel);
                } else {
                    final Product collocationProduct = collocationCrsUI.getCollocationProduct();
                    if (collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                        workCopy = new OutputGeometryFormModel(sourceProduct, collocationProduct);
                    } else {
                        workCopy = new OutputGeometryFormModel(sourceProduct, crs);
                    }
                }
                final OutputGeometryForm form = new OutputGeometryForm(workCopy);
                final ModalDialog outputParametersDialog = new OutputParametersDialog(appContext.getApplicationWindow(),
                        sourceProduct, workCopy);
                outputParametersDialog.setContent(form);
                if (outputParametersDialog.show() == ModalDialog.ID_OK) {
                    outputGeometryModel = workCopy;
                    updateProductSize();
                }
            } catch (Exception e) {
                appContext.handleError("Could not create a 'Coordinate Reference System'.\n" +
                        e.getMessage(), e);
            }
        }

    }

    private void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(getParent(), message, "Reprojection", JOptionPane.WARNING_MESSAGE);
    }

    private class OutputParametersDialog extends ModalDialog {

        private static final String TITLE = "Output Parameters";

        private final Product sourceProduct;
        private final OutputGeometryFormModel outputGeometryFormModel;

        public OutputParametersDialog(Window parent, Product sourceProduct,
                                      OutputGeometryFormModel outputGeometryFormModel) {
            super(parent, TITLE, ModalDialog.ID_OK_CANCEL | ModalDialog.ID_RESET, null);
            this.sourceProduct = sourceProduct;
            this.outputGeometryFormModel = outputGeometryFormModel;
        }

        @Override
        protected void onReset() {
            final Product collocationProduct = collocationCrsUI.getCollocationProduct();
            ImageGeometry imageGeometry;
            if (collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                imageGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
            } else {
                imageGeometry = ImageGeometry.createTargetGeometry(sourceProduct, crs,
                        null, null, null, null,
                        null, null, null, null, null);
            }
            outputGeometryFormModel.resetToDefaults(imageGeometry);
        }
    }

    private static class Model {

        private static final String PRESERVE_RESOLUTION = "preserveResolution";
        private static final String REPROJ_TIEPOINTS = "includeTiePointGrids";
        private static final String ADD_DELTA_BANDS = "addDeltaBands";
        private static final String NO_DATA_VALUE = "noDataValue";
        private static final String RESAMPLING_NAME = "resamplingName";

        private boolean preserveResolution = true;
        private boolean includeTiePointGrids = true;
        private boolean addDeltaBands = false;
        private double noDataValue = Double.NaN;
        private String resamplingName = RESAMPLING_IDENTIFIER[0];
    }

    private static class OrthorectifyProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            return product.canBeOrthorectified();
        }
    }

    private static class GeoCodingProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            final GeoCoding geoCoding = product.getGeoCoding();
            return geoCoding != null && geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos();
        }
    }
}
