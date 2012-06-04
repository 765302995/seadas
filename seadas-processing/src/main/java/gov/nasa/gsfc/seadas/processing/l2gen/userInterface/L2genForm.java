/*
Author: Danny Knowles
    Don Shea
*/

package gov.nasa.gsfc.seadas.processing.l2gen.userInterface;

import gov.nasa.gsfc.seadas.processing.core.*;
import gov.nasa.gsfc.seadas.processing.core.L2genData;
import gov.nasa.gsfc.seadas.processing.core.L2genParamCategoryInfo;
import gov.nasa.gsfc.seadas.processing.general.CloProgramUI;
import gov.nasa.gsfc.seadas.processing.general.GridBagConstraintsCustom;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;

//test
public class L2genForm extends JPanel implements CloProgramUI {

    private static final String GUI_NAME = "userInterface";

    private final L2genData l2genData = new L2genData();

    private L2genMainPanel l2genMainPanel;
    private ProcessorModel processorModel;

    private JCheckBox openInAppCheckBox;

    private final JTabbedPane jTabbedPane = new JTabbedPane();
    private int tabIndex;
//    private int mainTabIndex = 0;

    L2genForm(AppContext appContext, String xmlFileName) {

        processorModel = new ProcessorModel(GUI_NAME, xmlFileName);

        setOpenInAppCheckBox(new JCheckBox("Open in " + appContext.getApplicationName()));
        getOpenInAppCheckBox().setSelected(true);

        if (getL2genData().initXmlBasedObjects()) {

            createMainTab();
            createProductsTab();
            createCategoryParamTabs();

            tabIndex = jTabbedPane.getSelectedIndex();

            getjTabbedPane().addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    tabChangeHandler();
                }
            });


            setLayout(new GridBagLayout());

            add(getjTabbedPane(),
                    new GridBagConstraintsCustom(0, 0, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH));
            add(getOpenInAppCheckBox(),
                    new GridBagConstraintsCustom(0, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));


            getL2genData().disableEvent(L2genData.PARSTRING);
            getL2genData().disableEvent(L2genData.L2PROD);
            getL2genData().setInitialValues(getInitialSelectedSourceFile());

            getL2genData().fireAllParamEvents();
            getL2genData().enableEvent(L2genData.L2PROD);
            getL2genData().enableEvent(L2genData.PARSTRING);

        } else {
            add(new JLabel("Problem initializing userInterface"));
        }
    }

    private void tabChangeHandler() {
        int oldTabIndex = tabIndex;
        int newTabIndex = jTabbedPane.getSelectedIndex();
        tabIndex = newTabIndex;
        getL2genData().fireEvent(L2genData.TAB_CHANGE, oldTabIndex, newTabIndex);
    }


    private void createMainTab() {

        final String TAB_NAME = "Main";
        final int tabIndex = jTabbedPane.getTabCount();
        l2genMainPanel = new L2genMainPanel(this, tabIndex);
        jTabbedPane.addTab(TAB_NAME, l2genMainPanel);
    }


    private void createProductsTab() {

        final String TAB_NAME = "Products";
        L2genProductsPanel l2genProductsPanel = new L2genProductsPanel((getL2genData()));
        jTabbedPane.addTab(TAB_NAME, l2genProductsPanel);
        final int tabIndex = jTabbedPane.getTabCount() - 1;

        getL2genData().addPropertyChangeListener(L2genData.L2PROD, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                StringBuilder tabname = new StringBuilder(TAB_NAME);

                if (getL2genData().isParamDefault(L2genData.L2PROD)) {
                    jTabbedPane.setTitleAt(tabIndex, tabname.toString());
                } else {
                    jTabbedPane.setTitleAt(tabIndex, tabname.append("*").toString());
                }
            }
        });


        getL2genData().addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                jTabbedPane.setEnabledAt(tabIndex, getL2genData().isValidIfile());
            }
        });
    }


    private void createCategoryParamTabs() {

        for (final L2genParamCategoryInfo paramCategoryInfo : getL2genData().getParamCategoryInfos()) {
            if (paramCategoryInfo.isAutoTab() && (paramCategoryInfo.getParamInfos().size() > 0)) {

                L2genCategorizedParamsPanel l2genCategorizedParamsPanel = new L2genCategorizedParamsPanel(getL2genData(), paramCategoryInfo);
                jTabbedPane.addTab(paramCategoryInfo.getName(), l2genCategorizedParamsPanel);
                final int tabIndex = jTabbedPane.getTabCount() - 1;

                for (ParamInfo paramInfo : paramCategoryInfo.getParamInfos()) {
                    getL2genData().addPropertyChangeListener(paramInfo.getName(), new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            StringBuilder stringBuilder = new StringBuilder(paramCategoryInfo.getName());

                            if (getL2genData().isParamCategoryDefault(paramCategoryInfo)) {
                                jTabbedPane.setTitleAt(tabIndex, stringBuilder.toString());
                            } else {
                                jTabbedPane.setTitleAt(tabIndex, stringBuilder.append("*").toString());
                            }

                        }
                    });
                }


                getL2genData().addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        jTabbedPane.setEnabledAt(tabIndex, getL2genData().isValidIfile());
                    }
                });
            }
        }
    }


    public ProcessorModel getProcessorModel() {
        processorModel.setParString(getL2genData().getParString(false));
        processorModel.updateParamInfo("ofile", getL2genData().getParamValue(L2genData.OFILE));
        return processorModel;
    }


    public Product getInitialSelectedSourceProduct() {
        return VisatApp.getApp().getSelectedProduct();
    }

    public File getInitialSelectedSourceFile() {
        if (getInitialSelectedSourceProduct() != null) {
            return getInitialSelectedSourceProduct().getFileLocation();
        }
        return null;
    }

    public Product getSelectedSourceProduct() {
        if (l2genMainPanel != null) {
            return l2genMainPanel.getSelectedProduct();
        }
        return null;
    }


//    public Product getSelectedSourceProduct() {
//        FileInfo iFileInfo = l2genData.getParamFileInfo(L2genData.IFILE);
//
//
//        if (iFileInfo != null && iFileInfo.canRead()) {
//            Product product = new Product(iFileInfo.getName(), "DummyType", 10, 10);
//            product.setFileLocation(iFileInfo);
//            return product;
//        }
//
//        return null;
//    }


    void prepareShow() {
        if (l2genMainPanel != null) {
            l2genMainPanel.prepareShow();
        }
    }

    void prepareHide() {
        if (l2genMainPanel != null) {
            l2genMainPanel.prepareHide();
        }
    }

    public boolean isOpenOutputInApp() {
        if (getOpenInAppCheckBox() != null) {
            return getOpenInAppCheckBox().isSelected();
        }
        return true;
    }

    public JCheckBox getOpenInAppCheckBox() {
        return openInAppCheckBox;
    }

    public void setOpenInAppCheckBox(JCheckBox openInAppCheckBox) {
        this.openInAppCheckBox = openInAppCheckBox;
    }

    public JTabbedPane getjTabbedPane() {
        return jTabbedPane;
    }
//
//    public int getMainTabIndex() {
//        return mainTabIndex;
//    }

    public L2genData getL2genData() {
        return l2genData;
    }
}