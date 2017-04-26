package gov.nasa.gsfc.seadas.processing.core;

import gov.nasa.gsfc.seadas.processing.common.*;
import gov.nasa.gsfc.seadas.processing.core.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.core.ocssw.OCSSWClient;
import gov.nasa.gsfc.seadas.processing.core.ocssw.OCSSWOldModel;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.swing.event.SwingPropertyChangeSupport;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.lang.Boolean;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 3/16/12
 * Time: 2:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessorModel implements L2genDataProcessorModel, Cloneable {
    protected String programName;

    private ParamList paramList;
    private boolean acceptsParFile;
    private boolean hasGeoFile;

    private Set<String> primaryOptions;
    private String parFileOptionName;

    private boolean readyToRun;
    private final String runButtonPropertyName = "RUN_BUTTON_STATUS_CHANGED";
    private final String allparamInitializedPropertyName = "ALL_PARAMS_INITIALIZED";
    private final String l2prodProcessors = "l2mapgen l2brsgen l2bin l2bin_aquarius l3bin smigen";

    final String L1AEXTRACT_MODIS = "l1aextract_modis",
            L1AEXTRACT_MODIS_XML_FILE = "l1aextract_modis.xml",
            L1AEXTRACT_SEAWIFS = "l1aextract_seawifs",
            L1AEXTRACT_SEAWIFS_XML_FILE = "l1aextract_seawifs.xml",
            L1AEXTRACT_VIIRS = "l1aextract_viirs",
            L1AEXTRACT_VIIRS_XML_FILE = "l1aextract_viirs.xml",
            L2EXTRACT = "l2extract",
            L2EXTRACT_XML_FILE = "l2extract.xml";

    private ProcessorModel secondaryProcessor;
    private Pattern progressPattern;

    private ProcessorTypeInfo.ProcessorID processorID;

    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    private boolean multipleInputFiles;

    private boolean openInSeadas;

    private String prodParamName = "prod";

    private String[] cmdArrayPrefix;
    private String[] cmdArraySuffix;

    private boolean isIfileValid = false;
    OCSSW ocssw;

    public ProcessorModel(String name, OCSSW ocssw) {

        programName = name;
        this.ocssw = ocssw;
        acceptsParFile = false;
        hasGeoFile = false;
        readyToRun = false;
        multipleInputFiles = false;
        paramList = new ParamList();
        setParFileOptionName(ParamUtils.DEFAULT_PAR_FILE_NAME);
        processorID = ProcessorTypeInfo.getProcessorID(programName);
        primaryOptions = new HashSet<String>();
        primaryOptions.add("ifile");
        primaryOptions.add("ofile");
        progressPattern = Pattern.compile(ParamUtils.DEFAULT_PROGRESS_REGEX);
        setOpenInSeadas(false);
        setCommandArrayPrefix();
        setCommandArraySuffix();
    }

    public ProcessorModel(String name, String parXMLFileName, OCSSW ocssw) {
        this(name, ocssw);
        if (parXMLFileName != null && parXMLFileName.length() > 0) {
            setParamList(ParamUtils.computeParamList(parXMLFileName));
            acceptsParFile = ParamUtils.getOptionStatus(parXMLFileName, "hasParFile");
            setParFileOptionName(ParamUtils.getParFileOptionName(parXMLFileName));
            progressPattern = Pattern.compile(ParamUtils.getProgressRegex(parXMLFileName));
            hasGeoFile = ParamUtils.getOptionStatus(parXMLFileName, "hasGeoFile");
            setPrimaryOptions(ParamUtils.getPrimaryOptions(parXMLFileName));
            setOpenInSeadas(false);
            setCommandArrayPrefix();
            setCommandArraySuffix();
        }
    }

    public ProcessorModel(String name, ArrayList<ParamInfo> paramList, OCSSW ocssw) {
        this(name, ocssw);
        setParamList(paramList);
    }

    public static ProcessorModel valueOf(String programName, String xmlFileName, OCSSW ocssw) {
        ProcessorTypeInfo.ProcessorID processorID = ProcessorTypeInfo.getProcessorID(programName);
        switch (processorID) {
            case EXTRACTOR:
                return new Extractor_Processor(programName, xmlFileName, ocssw);
            case MODIS_L1B_PY:
                return new Modis_L1B_Processor(programName, xmlFileName, ocssw);
            case LONLAT2PIXLINE:
                return new LonLat2Pixels_Processor(programName, xmlFileName, ocssw);
            case SMIGEN:
                return new SMIGEN_Processor(programName, xmlFileName, ocssw);
            case L3MAPGEN:
                return new L3MAPGEN_Processor(programName, xmlFileName, ocssw);
            case L2BIN:
                return new L2Bin_Processor(programName, xmlFileName, ocssw);
            case L2BIN_AQUARIUS:
                return new L2Bin_Processor(programName, xmlFileName, ocssw);
            case L3BIN:
                return new L3Bin_Processor(programName, xmlFileName, ocssw);
            case OCSSW_INSTALLER:
                return new OCSSWInstaller_Processor(programName, xmlFileName, ocssw);
            default:
        }
        return new ProcessorModel(programName, xmlFileName, ocssw);
    }

    private void setCommandArrayPrefix() {

        if (programName.equals(OCSSWOldModel.OCSSW_INSTALLER)) {
            cmdArrayPrefix = new String[1];
            cmdArrayPrefix[0] = getProgramName();
            if (!OCSSWOldModel.isOCSSWExist()) {
                getCmdArrayPrefix()[0] = OCSSWOldModel.TMP_OCSSW_INSTALLER;
            } else {
                getCmdArrayPrefix()[0] = OCSSWOldModel.getOcsswInstallerScriptPath();
            }
        } else {
            cmdArrayPrefix = new String[4];
            getCmdArrayPrefix()[0] = OCSSWOldModel.getOcsswRunnerScriptPath();
            getCmdArrayPrefix()[1] = "--ocsswroot";
            getCmdArrayPrefix()[2] = OCSSWOldModel.getOcsswEnv();
            getCmdArrayPrefix()[3] = getProgramName();
        }
    }

    private void setCommandArraySuffix() {
        setCmdArraySuffix(new String[0]);
    }

    public void addParamInfo(ParamInfo info) {
        paramList.addInfo(info);
    }

    public void removeParamInfo(ParamInfo paramInfo) {
        paramList.removeInfo(paramInfo.getName());
    }

    public boolean isReadyToRun() {
        return readyToRun;
    }

    public void setReadyToRun(boolean readyToRun) {
        boolean oldValue = this.readyToRun;
        this.readyToRun = readyToRun;
        fireEvent(getRunButtonPropertyName(), oldValue, readyToRun);
    }

    public String getOfileName() {
        return getParamValue(getPrimaryOutputFileOptionName());
    }

    public boolean isMultipleInputFiles() {
        return multipleInputFiles;
    }

    public void setMultipleInputFiles(boolean multipleInputFiles) {
        this.multipleInputFiles = multipleInputFiles;
    }

    public void createsmitoppmProcessorModel(String ofileName) {
        ProcessorModel smitoppm = new ProcessorModel("smitoppm_4_ui", ocssw);
        smitoppm.setAcceptsParFile(false);
        ParamInfo pi1 = new ParamInfo("ifile", getParamValue(getPrimaryOutputFileOptionName()));
        pi1.setOrder(0);
        pi1.setType(ParamInfo.Type.IFILE);
        ParamInfo pi2 = new ParamInfo("ofile", ofileName);
        pi2.setOrder(1);
        pi2.setType(ParamInfo.Type.OFILE);
        smitoppm.addParamInfo(pi1);
        smitoppm.addParamInfo(pi2);
        setSecondaryProcessor(smitoppm);

    }

    public void addParamInfo(String name, String value, ParamInfo.Type type) {
        ParamInfo info = new ParamInfo(name, value, type);
        addParamInfo(info);
    }


    public void addParamInfo(String name, String value, ParamInfo.Type type, int order) {
        ParamInfo info = new ParamInfo(name, value, type);
        info.setOrder(order);
        addParamInfo(info);
    }

    public String getPrimaryInputFileOptionName() {
        for (String name : primaryOptions) {
            ParamInfo param = paramList.getInfo(name);
            if ((param != null) &&
                    (param.getType() == ParamInfo.Type.IFILE) &&
                    (!param.getName().toLowerCase().contains("geo"))) {
                return name;
            }
        }
        return null;
    }

    public String getPrimaryOutputFileOptionName() {
        for (String name : primaryOptions) {
            ParamInfo param = paramList.getInfo(name);
            if ((param != null) && (param.getType() == ParamInfo.Type.OFILE)) {
                return name;
            }
        }
        return null;
    }

    public boolean hasGeoFile() {
        return hasGeoFile;
    }

    public void setHasGeoFile(boolean hasGeoFile) {
        boolean oldValue = this.hasGeoFile;
        this.hasGeoFile = hasGeoFile;
        paramList.getPropertyChangeSupport().firePropertyChange("geofile", oldValue, hasGeoFile);
    }

    public boolean isValidProcessor() {
        SeadasLogger.getLogger().info("program location: " + OCSSWOldModel.getOcsswRunnerScriptPath());
        return OCSSWOldModel.getOcsswRunnerScriptPath() != null;
    }

    public String getProgramName() {
        return programName;
    }

    public ArrayList<ParamInfo> getProgramParamList() {
        return paramList.getParamArray();
    }

    public boolean hasPrimaryOutputFile() {
        String name = getPrimaryOutputFileOptionName();
        if (name == null) {
            return false;
        } else {
            return true;
        }
    }

    public void setAcceptsParFile(boolean acceptsParFile) {
        this.acceptsParFile = acceptsParFile;
    }

    public boolean acceptsParFile() {
        return acceptsParFile;
    }

    public void updateParamInfo(ParamInfo currentOption, String newValue) {
        updateParamInfo(currentOption.getName(), newValue);
        checkCompleteness();
    }

    protected void checkCompleteness() {
        boolean complete = true;

        for (ParamInfo param : paramList.getParamArray()) {
            if (param.getValue() == null || param.getValue().trim().length() == 0) {
                complete = false;
                break;
            }
        }

        if (complete) {
            fireEvent(getAllparamInitializedPropertyName(), false, true);
        }
    }

    public ParamInfo getParamInfo(String paramName) {
        return paramList.getInfo(paramName);
    }

    public String getParamValue(String paramName) {
        ParamInfo option = getParamInfo(paramName);
        if (option != null) {
            return option.getValue();
        }
        return null;
    }

    public void updateParamInfo(String paramName, String newValue) {

        ParamInfo option = getParamInfo(paramName);
        if (option != null) {
            String oldValue = option.getValue();
            option.setValue(newValue);
            checkCompleteness();
            if (!(oldValue.contains(newValue) && oldValue.trim().length() == newValue.trim().length())) {
                SeadasFileUtils.debug("property changed from " + oldValue + " to " + newValue);
                propertyChangeSupport.firePropertyChange(option.getName(), oldValue, newValue);
            }
        }
    }

    private void updateGeoFileStatus(String ifileName) {
        setHasGeoFile(false);
        if ((processorID == ProcessorTypeInfo.ProcessorID.L1BRSGEN
                || processorID == ProcessorTypeInfo.ProcessorID.L1MAPGEN
                || processorID == ProcessorTypeInfo.ProcessorID.MODIS_L1B_PY)) {
            String missionName = (new FileInfo(ifileName)).getMissionName();
            if (missionName != null && (missionName.indexOf("MODIS") != -1 || missionName.indexOf("VIIRSN") != -1 || missionName.indexOf("VIIRS") != -1)) {
                setHasGeoFile(true);
            }
        }
    }

    public boolean updateIFileInfo(String ifileName) {


        if (programName != null && (programName.equals("multilevel_processor") || programName.equals("multilevel_processor.py"))) {
            return true;
        }

        if (programName != null && verifyIFilePath(ifileName)) {
            String ofileName = ocssw.getOfileName(ifileName);
            SeadasLogger.getLogger().info("ofile name from finding next level name: " + ofileName);
            if (ofileName != null) {
                isIfileValid = true;
                updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                updateGeoFileInfo(ifileName);
                updateOFileInfo(getOFileFullPath(ofileName));
                updateParamValues(new File(ifileName));
            }
        } else {
            isIfileValid = false;
            updateParamInfo(getPrimaryOutputFileOptionName(), "" + "\n");
            removePropertyChangeListeners(getPrimaryInputFileOptionName());
            int result = VisatApp.getApp().showQuestionDialog("Cannot compute output file name. Would you like to continue anyway?", "test");
            if (result == 0) {
            } else {
                updateParamInfo(getPrimaryInputFileOptionName(), "" + "\n");
            }
        }
        return isIfileValid;
    }


    public boolean updateGeoFileInfo(String ifileName) {
        updateGeoFileStatus(ifileName);
        if (hasGeoFile()) {
            updateParamInfo("geofile", SeadasFileUtils.getGeoFileNameFromIFile(ifileName));
            return true;
        }
        return false;
    }


    public boolean updateOFileInfo(String newValue) {
        if (newValue != null && newValue.trim().length() > 0) {
            String ofile = getOFileFullPath(newValue);
            updateParamInfo(getPrimaryOutputFileOptionName(), ofile + "\n");
            setReadyToRun(newValue.trim().length() == 0 ? false : true);
            return true;
        }
        return false;
    }


    public void setParamValue(String name, String value) {
        SeadasFileUtils.debug("primary io file option names: " + getPrimaryInputFileOptionName() + " " + getPrimaryOutputFileOptionName());
        SeadasFileUtils.debug("set param value: " + name + " " + value);
        SeadasFileUtils.debug(name + " " + value);
        if (name.trim().equals(getPrimaryInputFileOptionName())) {
            updateIFileInfo(value);
        } else if (name.trim().equals(getPrimaryOutputFileOptionName())) {
            updateOFileInfo(getOFileFullPath(value));
        } else {
            updateParamInfo(name, value);
        }
    }


    public String[] getCmdArrayPrefix() {
        return cmdArrayPrefix;
    }

    public EventInfo[] eventInfos = {
            new EventInfo("none", this),
    };

    private EventInfo getEventInfo(String name) {
        for (EventInfo eventInfo : eventInfos) {
            if (name.equals(eventInfo.getName())) {
                return eventInfo;
            }
        }
        return null;
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        SeadasFileUtils.debug(" added property name: " + propertyName);
        if (propertyName != null) {
            EventInfo eventInfo = getEventInfo(propertyName);
            if (eventInfo == null) {
                propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
            } else {
                eventInfo.addPropertyChangeListener(listener);
            }
        }
    }


    public void removePropertyChangeListeners(String propertyName) {
        EventInfo eventInfo = getEventInfo(propertyName);
        PropertyChangeListener[] propertyListeners = propertyChangeSupport.getPropertyChangeListeners(propertyName);
        for (PropertyChangeListener propertyChangeListener : propertyListeners) {
            propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);

            if (eventInfo == null) {
                propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
            } else {
                eventInfo.removePropertyChangeListener(propertyChangeListener);
            }
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        EventInfo eventInfo = getEventInfo(propertyName);
        if (eventInfo == null) {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        } else {
            eventInfo.removePropertyChangeListener(listener);
        }
    }

    public void disableEvent(String name) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            SeadasLogger.getLogger().severe("disableEvent - eventInfo not found for " + name);
        } else {
            eventInfo.setEnabled(false);
        }
    }

    public void enableEvent(String name) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            SeadasLogger.getLogger().severe("enableEvent - eventInfo not found for " + name);
        } else {
            eventInfo.setEnabled(true);
        }
    }

    public void fireEvent(String name) {
        fireEvent(name, null, null);
    }

    public void fireEvent(String name, Object oldValue, Object newValue) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(this, name, oldValue, newValue));
        } else {
            eventInfo.fireEvent(oldValue, newValue);
        }
    }

    public void fireAllParamEvents() {
        for (ParamInfo paramInfo : paramList.getParamArray()) {
            if (paramInfo.getName() != null && !paramInfo.getName().toLowerCase().equals("none")) {
                fireEvent(paramInfo.getName());
            }
        }
    }

    public File getRootDir() {
        File rootDir = (new File(getParamValue(getPrimaryInputFileOptionName()))).getParentFile();
        if (rootDir != null) {
            return rootDir;
        } else {
            try {
                rootDir = new File(OCSSWOldModel.getOcsswRoot());
            } catch (Exception e) {
                SeadasLogger.getLogger().severe("error in getting ocssw root!");
            }

        }
        return rootDir == null ? new File(".") : rootDir;
    }

    public ProcessorModel getSecondaryProcessor() {
        return secondaryProcessor;
    }

    public void setSecondaryProcessor(ProcessorModel secondaryProcessor) {
        this.secondaryProcessor = secondaryProcessor;
    }

    public boolean isValidIfile() {
        return isIfileValid;
    }

    public boolean isGeofileRequired() {
        return hasGeoFile;
    }

    @Override
    public boolean isWavelengthRequired() {
        return true;
    }

    private boolean verifyIFilePath(String ifileName) {

        File ifile = new File(ifileName);

        if (ifile.exists()) {
            return true;
        }
        return false;
    }

    private String getIfileDirString() {
        String ifileDir;
        try {
            ifileDir = getParamValue(getPrimaryInputFileOptionName());
            ifileDir = ifileDir.substring(0, ifileDir.lastIndexOf(System.getProperty("file.separator")));
        } catch (Exception e) {
            ifileDir = System.getProperty("user.dir");
        }
        return ifileDir;
    }

    public File getIFileDir() {

        if (new File(getIfileDirString()).isDirectory()) {
            return new File(getIfileDirString());
        } else {
            return null;
        }
    }

    private String getOFileFullPath(String fileName) {
        if (fileName.indexOf(System.getProperty("file.separator")) == 0 && new File(fileName).getParentFile().exists()) {
            return fileName;
        } else if (new File(getIfileDirString(), fileName).getParentFile().exists()) {
            return getIfileDirString() + System.getProperty("file.separator") + fileName;

        } else {
            return null;
        }
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public ParamList getParamList() {
        return paramList;
    }

    public void setParamList(ParamList paramList) {
        this.paramList = paramList;
    }

    public void setParamList(ArrayList<ParamInfo> paramArray) {
        paramList.clear();
        for (ParamInfo param : paramArray) {
            paramList.addInfo(param);
        }
    }

    public SwingPropertyChangeSupport getPropertyChangeSupport() {
        return paramList.getPropertyChangeSupport();
    }

    public void appendPropertyChangeSupport(SwingPropertyChangeSupport propertyChangeSupport) {
        paramList.appendPropertyChangeSupport(propertyChangeSupport);
    }

    public Set<String> getPrimaryOptions() {
        return primaryOptions;
    }

    public void setPrimaryOptions(Set<String> primaryOptions) {
        this.primaryOptions = primaryOptions;
    }

    public String getRunButtonPropertyName() {
        return runButtonPropertyName;
    }

    public String getAllparamInitializedPropertyName() {
        return allparamInitializedPropertyName;
    }

    private String executionLogMessage;

    public String getExecutionLogMessage() {
        return executionLogMessage;
    }

    public void setExecutionLogMessage(String executionLogMessage) {
        this.executionLogMessage = executionLogMessage;
    }

    public void setProgressPattern(Pattern progressPattern) {
        this.progressPattern = progressPattern;
    }

    public Pattern getProgressPattern() {
        return progressPattern;
    }

    public boolean isOpenInSeadas() {
        return openInSeadas;
    }

    public void setOpenInSeadas(boolean openInSeadas) {
        this.openInSeadas = openInSeadas;
    }

    String getProdParamName() {
        return prodParamName;
    }

    void setProdPramName(String prodPramName) {
        this.prodParamName = prodPramName;
    }

    public void updateParamValues(Product selectedProduct) {
        updateParamValues(selectedProduct.getFileLocation());
    }

    public void updateParamValues(File selectedFile) {

        if (selectedFile == null || (programName != null && !l2prodProcessors.contains(programName))) {
            return;
        }

        if (selectedFile.getName().endsWith(".txt")) {
            try {
                LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(selectedFile));
                String sampleFileName = lineNumberReader.readLine();
                if (new File(sampleFileName).exists()) {
                    selectedFile = new File(sampleFileName);
                    //System.out.println("sample file name: " + sampleFileName + System.currentTimeMillis());
                } else {
                    return;
                }
            } catch (FileNotFoundException fnfe) {

            } catch (IOException ioe) {

            }
        }

        NetcdfFile ncFile = null;
        try {
            ncFile = NetcdfFile.open(selectedFile.getAbsolutePath());
        } catch (IOException ioe) {

        }

        ArrayList<String> products = new ArrayList<String>();
        if (ncFile != null) {
            java.util.List<Variable> var = null;

            List<ucar.nc2.Group> groups = ncFile.getRootGroup().getGroups();
            for (ucar.nc2.Group g : groups) {
                //retrieve geophysical data to fill in "product" value ranges
                if (g.getShortName().equalsIgnoreCase("Geophysical_Data")) {
                    var = g.getVariables();
                    break;
                }
            }
            if (var != null) {
                for (Variable v : var) {
                    products.add(v.getShortName());
                }
                String[] bandNames = new String[products.size()];
                products.toArray(bandNames);

                ParamInfo pi = getParamInfo(getProdParamName());
                if (bandNames != null && pi != null) {
                    ArrayList<ParamValidValueInfo> oldValidValues = (ArrayList<ParamValidValueInfo>) pi.getValidValueInfos().clone();
                    String oldValue = pi.getValue();
                    ParamValidValueInfo paramValidValueInfo;
                    for (String bandName : bandNames) {
                        paramValidValueInfo = new ParamValidValueInfo(bandName);
                        paramValidValueInfo.setDescription(bandName);
                        pi.addValidValueInfo(paramValidValueInfo);
                    }
                    ArrayList<ParamValidValueInfo> newValidValues = pi.getValidValueInfos();
                    String newValue = pi.getValue() != null ? pi.getValue() : newValidValues.get(0).getValue();
                    paramList.getPropertyChangeSupport().firePropertyChange(getProdParamName(), oldValue, newValue);
                }
            }
        }
    }

    public String[] getCmdArraySuffix() {
        return cmdArraySuffix;
    }

    public void setCmdArraySuffix(String[] cmdArraySuffix) {
        this.cmdArraySuffix = cmdArraySuffix;
    }

    public String getParFileOptionName() {
        return parFileOptionName;
    }

    public void setParFileOptionName(String parFileOptionName) {
        this.parFileOptionName = parFileOptionName;
    }


    private static class Extractor_Processor extends ProcessorModel {
        Extractor_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
        }

        @Override
        public boolean updateIFileInfo(String ifileName) {
            updateExtractor(ifileName);
            return super.updateIFileInfo(ifileName);
        }

        private void updateExtractor(String ifileName) {
            FileInfo ifileInfo = new FileInfo(ifileName);
            SeadasFileUtils.debug("Extractor ifile info in extractor class method: " + ifileInfo.getTypeName() + ifileInfo.getMissionName());
            String programName = null;
            String xmlFileName = null;
            if (ifileInfo.getMissionName() != null && ifileInfo.getTypeName() != null) {
                if (ifileInfo.getMissionName().indexOf("MODIS") != -1 && ifileInfo.getTypeName().indexOf("1A") != -1) {
                    programName = L1AEXTRACT_MODIS;
                    xmlFileName = L1AEXTRACT_MODIS_XML_FILE;
                } else if (ifileInfo.getMissionName().indexOf("SeaWiFS") != -1 && ifileInfo.getTypeName().indexOf("1A") != -1 ||
                        ifileInfo.getMissionName().indexOf("CZCS") != -1) {
                    programName = L1AEXTRACT_SEAWIFS;
                    xmlFileName = L1AEXTRACT_SEAWIFS_XML_FILE;
                } else if (ifileInfo.getMissionName().indexOf("VIIRS") != -1 && ifileInfo.getTypeName().indexOf("1A") != -1) {
                    programName = L1AEXTRACT_VIIRS;
                    xmlFileName = L1AEXTRACT_VIIRS_XML_FILE;
                } else if ((ifileInfo.getTypeName().indexOf("L2") != -1 || ifileInfo.getTypeName().indexOf("Level 2") != -1) ||
                        (ifileInfo.getMissionName().indexOf("OCTS") != -1 && (ifileInfo.getTypeName().indexOf("L1") != -1 || ifileInfo.getTypeName().indexOf("Level 1") != -1))) {
                    programName = L2EXTRACT;
                    xmlFileName = L2EXTRACT_XML_FILE;
                }
            }
            setProgramName(programName);
            if (programName != null) {
                setParamList(ParamUtils.computeParamList(xmlFileName));
                getCmdArrayPrefix()[3]=programName;

            } else {
                VisatApp.getApp().showErrorDialog("No extractor found for " + ifileName);
            }
        }
    }

    private static class Modis_L1B_Processor extends ProcessorModel {
        Modis_L1B_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
        }

        public boolean updateOFileInfo(String ofileName) {
            updateParamInfo("--okm", ofileName.replaceAll("LAC", "LAC"));
            getParamInfo("--okm").setDefaultValue(getParamValue("--okm"));
            updateParamInfo("--hkm", ofileName.replaceAll("LAC", "HKM"));
            getParamInfo("--hkm").setDefaultValue(getParamValue("--hkm"));
            updateParamInfo("--qkm", ofileName.replaceAll("LAC", "QKM"));
            getParamInfo("--qkm").setDefaultValue(getParamValue("--qkm"));
            updateParamInfo("--obc", ofileName.replaceAll("LAC", "OBC"));
            getParamInfo("--obc").setDefaultValue(getParamValue("--obc"));
            setReadyToRun(ofileName.trim().length() == 0 ? false : true);
            return true;
        }

        public String getOfileName() {

            StringBuilder ofileNameList = new StringBuilder();
            if (!(getParamInfo("--del-okm").getValue().equals("true") || getParamInfo("--del-okm").getValue().equals("1"))) {
                ofileNameList.append("\n" + getParamValue("--okm"));
            }
            if (!(getParamInfo("--del-hkm").getValue().equals("true") || getParamInfo("--del-hkm").getValue().equals("1"))) {
                ofileNameList.append("\n" + getParamValue("--hkm"));
            }
            if (!(getParamInfo("--del-qkm").getValue().equals("true") || getParamInfo("--del-qkm").getValue().equals("1"))) {
                ofileNameList.append("\n" + getParamValue("--qkm"));
            }
            if (getParamInfo("--keep-obc").getValue().equals("true") || getParamInfo("--keep-obc").getValue().equals("1")) {
                ofileNameList.append("\n" + getParamValue("--obc"));
            }
            return ofileNameList.toString();
        }
    }

    private static class LonLat2Pixels_Processor extends ProcessorModel {
        static final String _SWlon = "SWlon";
        static final String _SWlat = "SWlat";
        static final String _NElon = "NElon";
        static final String _NElat = "NElat";

        private LonLat2PixlineConverter lonLat2PixlineConverter;

        LonLat2Pixels_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            addPropertyChangeListener("ifile", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_SWlon, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_SWlat, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_NElon, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_NElat, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();
                }
            });
        }


        @Override
        public void checkCompleteness() {
            String valueOfSWlon = getParamList().getInfo(_SWlon).getValue();
            String valueOfSWlat = getParamList().getInfo(_SWlat).getValue();
            String valueOfNElon = getParamList().getInfo(_NElon).getValue();
            String valueOfNElat = getParamList().getInfo(_NElat).getValue();

            if ((valueOfSWlon != null && valueOfSWlon.trim().length() > 0) &&
                    (valueOfSWlat != null && valueOfSWlat.trim().length() > 0) &&
                    (valueOfNElon != null && valueOfNElon.trim().length() > 0) &&
                    (valueOfNElat != null && valueOfNElat.trim().length() > 0)) {
                lonLat2PixlineConverter = new LonLat2PixlineConverter(this);
                lonLat2PixlineConverter.computePixelsFromLonLat();
                if (lonLat2PixlineConverter.computePixelsFromLonLat()) {
                    updateParamInfo(LonLat2PixlineConverter.START_PIXEL_PARAM_NAME, lonLat2PixlineConverter.getSpixl());
                    updateParamInfo(LonLat2PixlineConverter.END_PIXEL_PARAM_NAME, lonLat2PixlineConverter.getEpixl());
                    updateParamInfo(LonLat2PixlineConverter.START_LINE_PARAM_NAME, lonLat2PixlineConverter.getSline());
                    updateParamInfo(LonLat2PixlineConverter.END_LINE_PARAM_NAME, lonLat2PixlineConverter.getEline());
                    fireEvent(getAllparamInitializedPropertyName(), false, true);
                }
            }
        }

        public void updateParamInfo(String paramName, String newValue) {

            ParamInfo option = getParamInfo(paramName);
            if (option != null) {
                option.setValue(newValue);
            }
        }

        public boolean updateIFileInfo(String ifileName) {
            updateParamInfo(getPrimaryInputFileOptionName(), ifileName);
            updateGeoFileInfo(ifileName);
            return true;
        }

        public LonLat2PixlineConverter getLonLat2PixlineConverter() {
            return lonLat2PixlineConverter;
        }

        public void setLonLat2PixlineConverter(LonLat2PixlineConverter lonLat2PixlineConverter) {
            this.lonLat2PixlineConverter = lonLat2PixlineConverter;
        }
    }

    private static class L2Bin_Processor extends ProcessorModel {

        private static final String DEFAULT_PAR_FILE_NAME = "l2bin_defaults.par";
        private static final String PAR_FILE_PREFIX = "l2bin_defaults_";
        String DEFAULT_FLAGUSE;
        File missionDir;
        FileInfo ifileInfo;

        L2Bin_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setProdPramName("l3bprod");
            setMultipleInputFiles(true);
            missionDir = null;
        }

        @Override
        public void updateParamValues(Product selectedProduct) {
            if (selectedProduct != null) {
                String sampleFileName = selectedProduct.getFileLocation().getAbsolutePath();
                ifileInfo = new FileInfo(sampleFileName);
                if (ifileInfo.getMissionId().equals(MissionInfo.Id.UNKNOWN)) {
                    try (BufferedReader br = new BufferedReader(new FileReader(sampleFileName))) {
                        String listedFileName;
                        while ((listedFileName = br.readLine()) != null) {
                            ifileInfo = new FileInfo(listedFileName);
                            if (!ifileInfo.getMissionId().equals(MissionInfo.Id.UNKNOWN)) {
                                break;
                            }
                        }
                    } catch (Exception e) {

                    }
                }
                missionDir = ifileInfo.getMissionDirectory();
                if (missionDir == null) {
                    try {
                        LineNumberReader reader = new LineNumberReader(new FileReader(new File(selectedProduct.getFileLocation().getAbsolutePath())));
                        sampleFileName = reader.readLine();
                        missionDir = new FileInfo(sampleFileName).getMissionDirectory();
                    } catch (FileNotFoundException fnfe) {

                    } catch (IOException ioe) {

                    }

                }
                DEFAULT_FLAGUSE = SeadasFileUtils.getKeyValueFromParFile(new File(missionDir, DEFAULT_PAR_FILE_NAME), "flaguse");
                updateSuite();
                super.updateParamValues(new File(sampleFileName));
            }
        }

        private void updateSuite() {

            String[] suites;
            HashMap<String, Boolean> missionSuites;
            if (OCSSWOldModel.isOCSSWInstalledLocal()) {
                suites = missionDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return s.contains("l2bin_defaults_");
                    }
                });
            } else {
                OCSSWClient ocsswClient = new OCSSWClient();
                WebTarget target = ocsswClient.getOcsswWebTarget();
                missionSuites = target.path("ocssw").path("l2bin_suites").path(ifileInfo.getMissionName()).request(MediaType.APPLICATION_JSON)
                        .get(new GenericType<HashMap<String, Boolean>>() {
                        });
                int i = 0;
                suites = new String[missionSuites.size()];
                for (Map.Entry<String, Boolean> entry : missionSuites.entrySet()) {
                    String missionName = entry.getKey();
                    Boolean missionStatus = entry.getValue();

                    if (missionStatus) {
                        suites[i++] = missionName;
                    }

                }
            }
            String suiteName;
            ArrayList<ParamValidValueInfo> suiteValidValues = new ArrayList<ParamValidValueInfo>();
            for (String fileName : suites) {
                suiteName = fileName.substring(fileName.indexOf("_", fileName.indexOf("_") + 1) + 1, fileName.indexOf("."));
                suiteValidValues.add(new ParamValidValueInfo(suiteName));
            }
            ArrayList<ParamValidValueInfo> oldValidValues = (ArrayList<ParamValidValueInfo>) getParamInfo("suite").getValidValueInfos().clone();
            getParamInfo("suite").setValidValueInfos(suiteValidValues);
            fireEvent("suite", oldValidValues, suiteValidValues);
            updateFlagUse(DEFAULT_PAR_FILE_NAME);
        }

        @Override
        public void updateParamInfo(ParamInfo currentOption, String newValue) {

            if (currentOption.getName().equals("suite")) {
                updateFlagUse(PAR_FILE_PREFIX + newValue + ".par");
            }
            super.updateParamInfo(currentOption, newValue);
        }

        private void updateFlagUse(String parFileName) {
            String currentFlagUse = SeadasFileUtils.getKeyValueFromParFile(new File(missionDir, parFileName), "flaguse");
            if (currentFlagUse == null) {
                currentFlagUse = DEFAULT_FLAGUSE;
            }
            if (currentFlagUse != null) {
                ArrayList<ParamValidValueInfo> validValues = getParamInfo("flaguse").getValidValueInfos();
                for (ParamValidValueInfo paramValidValueInfo : validValues) {
                    if (currentFlagUse.contains(paramValidValueInfo.getValue().trim())) {
                        paramValidValueInfo.setSelected(true);
                    } else {
                        paramValidValueInfo.setSelected(false);
                    }
                }
                super.updateParamInfo("flaguse", currentFlagUse);
                fireEvent("flaguse", null, currentFlagUse);
            }
        }
    }

    private static class L2BinAquarius_Processor extends ProcessorModel {
        L2BinAquarius_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setMultipleInputFiles(true);
        }
    }

    private static class L3Bin_Processor extends ProcessorModel {
        L3Bin_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setMultipleInputFiles(true);
            addPropertyChangeListener("out_parm", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String oldProdValue = (String) propertyChangeEvent.getOldValue();
                    String newProdValue = (String) propertyChangeEvent.getNewValue();
                    String ofileName = getParamValue(getPrimaryOutputFileOptionName());
                    if (oldProdValue.trim().length() > 0 && ofileName.indexOf(oldProdValue) != -1) {
                        ofileName = ofileName.replaceAll(oldProdValue, newProdValue);
                    } else {
                        ofileName = ofileName + "_" + newProdValue;
                    }
                    updateOFileInfo(ofileName);
                }
            });
        }

        public String getOfileName() {
            if (!(getParamValue("noext").equals("1"))) {
                return getParamValue(getPrimaryOutputFileOptionName()) + ".main";
            }
            return getParamValue(getPrimaryOutputFileOptionName());
        }
    }


    private static class SMIGEN_Processor extends ProcessorModel {
        SMIGEN_Processor(final String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setOpenInSeadas(true);
            addPropertyChangeListener("prod", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    if (ifileName != null) {
                        String oldProdValue = (String) propertyChangeEvent.getOldValue();
                        String newProdValue = (String) propertyChangeEvent.getNewValue();
                        String[] additionalOptions = {"--suite=" + newProdValue, "--resolution=" + getParamValue("resolution")};
                        //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                        String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                        updateOFileInfo(ofileName);
                    }
                }
            });

            addPropertyChangeListener("resolution", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String oldResolutionValue = (String) propertyChangeEvent.getOldValue();
                    String newResolutionValue = (String) propertyChangeEvent.getNewValue();
                    String suite = getParamValue("prod");
                    if (suite==null || suite.trim().length()==0) {
                        suite = "all";
                    }
                    String[] additionalOptions = {"--resolution=" + newResolutionValue, "--suite=" + suite};
                    //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                    String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                    updateOFileInfo(ofileName);
                }
            });
        }

    }


    private static class L3MAPGEN_Processor extends ProcessorModel {
        L3MAPGEN_Processor(final String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setOpenInSeadas(false);

            addPropertyChangeListener("product", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    if (ifileName != null) {
                        String oldProdValue = (String) propertyChangeEvent.getOldValue();
                        String newProdValue = (String) propertyChangeEvent.getNewValue();
                        String[] additionalOptions = {"--suite=" + newProdValue, "--resolution=" + getParamValue("resolution"), "--oformat=" + getParamValue("oformat")};
                        String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                        //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                        updateOFileInfo(ofileName);
                    }
                }
            });

            addPropertyChangeListener("resolution", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String oldResolutionValue = (String) propertyChangeEvent.getOldValue();
                    String newResolutionValue = (String) propertyChangeEvent.getNewValue();
                    String suite = getParamValue("product");
                    if (suite==null || suite.trim().length()==0) {
                        suite = "all";
                    }
                    String[] additionalOptions = {"--resolution=" + newResolutionValue, "--suite=" + suite, "--oformat=" + getParamValue("oformat")};
                    //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                    String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("oformat", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String oldFormatValue = (String) propertyChangeEvent.getOldValue();
                    String newFormatValue = (String) propertyChangeEvent.getNewValue();
                    String suite = getParamValue("product");
                    if (suite==null || suite.trim().length()==0) {
                        suite = "all";
                    }
                    String[] additionalOptions = {"--resolution=" + getParamValue("resolution"), "--suite=" + suite, "--oformat=" + newFormatValue};
                    //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                    String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                    updateOFileInfo(ofileName);
                }
            });

        }

    }

    private static class OCSSWInstaller_Processor extends ProcessorModel {
        OCSSWInstaller_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
        }

        /**
         * The version number of the "git-branch" is the first two digits of the SeaDAS app number; trailing numbers should be ignored.
         * For example, for SeaDAS 7.3.2, the "git-branch" option on command line should be prepared as "--git-branch=v7.3".
         *
         * @return
         */
        @Override
        public String[] getCmdArraySuffix() {
            String[] cmdArraySuffix = new String[1];
            String[] parts = VisatApp.getApp().getAppVersion().split("\\.");
            cmdArraySuffix[0] = "--git-branch=v" + parts[0] + "." + parts[1];
            ocssw.setCommandArraySuffix(cmdArraySuffix);
            return cmdArraySuffix;
        }
    }

}


