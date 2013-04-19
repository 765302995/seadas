/*
Author: Danny Knowles
    Don Shea
*/

package gov.nasa.gsfc.seadas.processing.l2gen.userInterface;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.processing.core.*;
import gov.nasa.gsfc.seadas.processing.general.*;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


public class L2genForm extends JPanel implements CloProgramUI {


    private L2genData l2genData;

    private L2genMainPanel l2genMainPanel;
    private JCheckBox openInAppCheckBox;
    private final JTabbedPane jTabbedPane = new JTabbedPane();
    private int tabIndex;


    public L2genForm(AppContext appContext, String xmlFileName, final File iFile, boolean showIOFields, L2genData.Mode mode) {

        l2genData = new L2genData(mode);

        switch (mode) {
            case L2GEN_AQUARIUS:
                l2genData.setGuiName(L2genData.GUI_NAME_AQUARIUS);
                break;
            default:
              l2genData.setGuiName(L2genData.GUI_NAME);
                break;
        }

        setOpenInAppCheckBox(new JCheckBox("Open in " + appContext.getApplicationName()));
        getOpenInAppCheckBox().setSelected(true);

        l2genData.showIOFields = showIOFields;


        VisatApp visatApp = VisatApp.getApp();
        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(visatApp.getMainFrame(),
               l2genData.getGuiName()) {

            @Override
            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                pm.beginTask("Initializing " + l2genData.getGuiName(), 2);

                try {

                    getL2genData().initXmlBasedObjects();

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

                    if (iFile != null) {
                        getL2genData().setInitialValues(iFile);
                    } else {
                        getL2genData().setInitialValues(getInitialSelectedSourceFile());
                    }


                    getL2genData().fireAllParamEvents();
                    getL2genData().enableEvent(L2genData.L2PROD);
                    getL2genData().enableEvent(L2genData.PARSTRING);
                    pm.worked(1);

                } catch (IOException e) {
                    pm.done();
                    SimpleDialogMessage dialog = new SimpleDialogMessage(null, "ERROR - Problem " + e.getMessage());
                    dialog.setVisible(true);
                    dialog.setEnabled(true);


                } finally {
                    pm.done();
                }
                return null;
            }


        };

        pmSwingWorker.executeWithBlocking();

        l2genData.setInitialized(true);

    }

    public L2genForm(AppContext appContext, String xmlFileName) {

        this(appContext, xmlFileName, null, true, L2genData.Mode.L2GEN);
    }

    public L2genForm(AppContext appContext, String xmlFileName, L2genData.Mode mode) {

        this(appContext, xmlFileName, null, true, mode);
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
        l2genMainPanel = new L2genMainPanel(l2genData, tabIndex);
        jTabbedPane.addTab(TAB_NAME, l2genMainPanel.getjPanel());
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

                /*
                    Add titles to each of the tabs, adding (*) where tab contains a non-default parameter
                 */
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

    @Override
    public JPanel getParamPanel() {
        return this;
    }

    public ProcessorModel getProcessorModel() {
        return l2genData.getProcessorModel();
    }

    public String getParamString() {
        return l2genData.getParString();
    }

    public void setParamString(String paramString) {
        l2genData.setParString(paramString, false);
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


    public SourceProductFileSelector getSourceProductSelector() {
        return l2genMainPanel.getPrimaryIOFilesPanel().getIfileSelector().getSourceProductSelector();
    }


    public Product getSelectedSourceProduct() {
        if (getSourceProductSelector() != null) {
            return getSourceProductSelector().getSelectedProduct();
        }

        return null;
    }


    public void prepareShow() {
        if (getSourceProductSelector() != null) {
            getSourceProductSelector().initProducts();
        }
    }

    public void prepareHide() {
        if (getSourceProductSelector() != null) {
            getSourceProductSelector().releaseProducts();
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


    public L2genData getL2genData() {
        return l2genData;
    }


}