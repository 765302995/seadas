package gov.nasa.gsfc.seadas.processing.l2gen.userInterface;

import gov.nasa.gsfc.seadas.processing.core.L2genDataProcessorModel;
import gov.nasa.gsfc.seadas.processing.general.FileSelector;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: knowles
 * Date: 6/6/12
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class L2genOfileSelector {

    final private L2genDataProcessorModel l2genDataProcessorModel;

    final private FileSelector fileSelector;
    private boolean controlHandlerEnabled = true;

    public L2genOfileSelector(L2genDataProcessorModel  l2genDataProcessorModel) {
        this.l2genDataProcessorModel = l2genDataProcessorModel;

        fileSelector = new FileSelector(VisatApp.getApp(), FileSelector.Type.OFILE, l2genDataProcessorModel.getPrimaryOutputFileOptionName());

        addControlListeners();
        addEventListeners();
    }

    private void addControlListeners() {
        fileSelector.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (isControlHandlerEnabled()) {
                    l2genDataProcessorModel.setParamValue(l2genDataProcessorModel.getPrimaryOutputFileOptionName(), fileSelector.getFileName());
                    disableControlHandler();
                }
            }
        });

    }

    private void addEventListeners() {
        l2genDataProcessorModel.addPropertyChangeListener(l2genDataProcessorModel.getPrimaryOutputFileOptionName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //disableControlHandler();
                fileSelector.setFilename(l2genDataProcessorModel.getParamValue(l2genDataProcessorModel.getPrimaryOutputFileOptionName()));
                enableControlHandler();
            }
        });

        l2genDataProcessorModel.addPropertyChangeListener(l2genDataProcessorModel.getPrimaryOutputFileOptionName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fileSelector.setEnabled(l2genDataProcessorModel.isValidIfile());
            }
        });
    }

    private boolean isControlHandlerEnabled() {
        return controlHandlerEnabled;
    }

    private void enableControlHandler() {
        controlHandlerEnabled = true;
    }

    private void disableControlHandler() {
        controlHandlerEnabled = false;
    }


    public JPanel getJPanel() {
        return fileSelector.getjPanel();
    }

    public FileSelector getFileSelector() {
        return fileSelector;
    }
}
