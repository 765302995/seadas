package gov.nasa.gsfc.seadas.processing.core;


import gov.nasa.gsfc.seadas.processing.general.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.l2gen.*;
import org.esa.beam.util.StringUtils;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;

/**
 * A ...
 *
 * @author Danny Knowles
 * @since SeaDAS 7.0
 */
public class L2genData {


    //  private static final String OCDATAROOT = System.getenv("OCDATAROOT");
    private static final File DEFAULT_IFILE = new File("S2002032172026.L1A_GAC.reallysmall");

    private static final String
            PRODUCT_INFO_XML = "productInfo.xml",
            PARAM_INFO_XML = "paramInfo.xml",
            PARAM_CATEGORY_INFO_XML = "paramCategoryInfo.xml",
            PRODUCT_CATEGORY_INFO_XML = "productCategoryInfo.xml";

    public static final String
            PAR = "par",
            GEOFILE = "geofile",
            SPIXL = "spixl",
            EPIXL = "epixl",
            SLINE = "sline",
            ELINE = "eline",
            NORTH = "north",
            SOUTH = "south",
            WEST = "west",
            EAST = "east",
            IFILE = "ifile",
            OFILE = "ofile",
            L2PROD = "l2prod";

    public static final String
            INVALID_IFILE = "INVALID_IFILE_EVENT",
            WAVE_LIMITER = "WAVE_LIMITER_EVENT",
            RETAIN_IFILE = "RETAIN_IFILE_EVENT",
            SHOW_DEFAULTS = "SHOW_DEFAULTS_EVENT",
            PARSTRING = "PARSTRING_EVENT",
            TAB_CHANGE = "TAB_CHANGE_EVENT";


    public FileInfo iFileInfo = null;

    public final ArrayList<WavelengthInfo> waveLimiterInfos = new ArrayList<WavelengthInfo>();

    private final L2genReader l2genReader = new L2genReader(this);

    private final ArrayList<ParamInfo> paramInfos = new ArrayList<ParamInfo>();


    private final ArrayList<L2genParamCategoryInfo> paramCategoryInfos = new ArrayList<L2genParamCategoryInfo>();

    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    private final SeadasPrint l2genPrint = new SeadasPrint();

    private int currentTabIndex = 0;


    // useful shortcuts to popular paramInfos
    private final HashMap<String, ParamInfo> paramInfoHashMap = new HashMap<String, ParamInfo>();
    private L2genProductsParamInfo l2prodParamInfo = null;
    private ParamInfo ofileParamInfo = null;
    private ParamInfo ifileParamInfo = null;


    public boolean retainCurrentIfile = true;
    private boolean showDefaultsInParString = false;

    public void initParamInfoHashMap() {
        paramInfoHashMap.clear();
        for (ParamInfo paramInfo : paramInfos) {
            paramInfoHashMap.put(paramInfo.getName().toLowerCase(), paramInfo);
        }
    }

    public boolean isRetainCurrentIfile() {
        return retainCurrentIfile;
    }

    public void setRetainCurrentIfile(boolean retainCurrentIfile) {

        if (this.retainCurrentIfile != retainCurrentIfile) {
            this.retainCurrentIfile = retainCurrentIfile;
            fireEvent(RETAIN_IFILE);
        }
    }

    public boolean isShowDefaultsInParString() {
        return showDefaultsInParString;
    }

    public void setShowDefaultsInParString(boolean showDefaultsInParString) {
        if (this.showDefaultsInParString != showDefaultsInParString) {
            this.showDefaultsInParString = showDefaultsInParString;
            fireEvent(SHOW_DEFAULTS);
        }
    }

    public boolean isValidIfile() {

        if (iFileInfo != null && iFileInfo.exists() && iFileInfo.isAbsolute()) {
            if (iFileInfo.isMissionId(MissionInfo.Id.MODISA) ||
                    iFileInfo.isMissionId(MissionInfo.Id.MODIST) ||
                    iFileInfo.isMissionId(MissionInfo.Id.MERIS)
                    ) {
                if (iFileInfo.isTypeId(FileTypeInfo.Id.L1B)) {
                    return true;
                }

            } else if (iFileInfo.isSupportedMission()) {
                if (iFileInfo.isTypeId(FileTypeInfo.Id.L1A) ||
                        iFileInfo.isTypeId(FileTypeInfo.Id.L1B)) {
                    return true;
                }
            }
        }

        return false;
    }


    public boolean isRequiresGeofile() {
        if (iFileInfo != null) {
            return iFileInfo.isGeofileRequired();
        }

        return false;
    }

    public ParamInfo getOfileParamInfo() {
        if (ofileParamInfo == null) {
            return getParamInfo(OFILE);
        }
        return ofileParamInfo;
    }

    public void setOfileParamInfo(ParamInfo ofileParamInfo) {
        this.ofileParamInfo = ofileParamInfo;
    }

    public ParamInfo getIfileParamInfo() {
        if (ifileParamInfo == null) {
            return getParamInfo(IFILE);
        }
        return ifileParamInfo;
    }

    public void setIfileParamInfo(ParamInfo ifileParamInfo) {
        this.ifileParamInfo = ifileParamInfo;
    }


    public enum RegionType {Coordinates, PixelLines}

    public EventInfo[] eventInfos = {
            new EventInfo(L2PROD, this),
            new EventInfo(PARSTRING, this)
    };

    public L2genData() {
    }

    private EventInfo getEventInfo(String name) {
        for (EventInfo eventInfo : eventInfos) {
            if (name.equals(eventInfo.getName())) {
                return eventInfo;
            }
        }
        return null;
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        EventInfo eventInfo = getEventInfo(propertyName);
        if (eventInfo == null) {
            propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        } else {
            eventInfo.addPropertyChangeListener(listener);
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
            debug("disableEvent - eventInfo not found for " + name);
        } else {
            eventInfo.setEnabled(false);
        }
    }

    public void enableEvent(String name) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            debug("enableEvent - eventInfo not found for " + name);
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
        //   fireEvent(PARSTRING_IN_PROGRESS_EVENT);
        disableEvent(PARSTRING);
        disableEvent(L2PROD);

        for (ParamInfo paramInfo : paramInfos) {
            //    if (paramInfo.getName() != null && !paramInfo.getName().toLowerCase().equals(IFILE)) {
            if (paramInfo.getName() != null) {
                fireEvent(paramInfo.getName());
            }
        }
        fireEvent(SHOW_DEFAULTS);
        fireEvent(RETAIN_IFILE);
        fireEvent(WAVE_LIMITER);
        fireEvent(PARSTRING);

        enableEvent(L2PROD);
        enableEvent(PARSTRING);

    }

    public void setSelectedInfo(BaseInfo info, BaseInfo.State state) {

        if (state != info.getState()) {
            info.setState(state);
            l2prodParamInfo.updateValue();
            fireEvent(L2PROD);
        }
    }


    /**
     * Set wavelength in waveLimiterInfos based on GUI change
     *
     * @param selectedWavelength
     * @param selected
     */
    public void setSelectedWaveLimiter(String selectedWavelength, boolean selected) {

        for (WavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (selectedWavelength.equals(waveLimiterInfo.getWavelengthString())) {
                if (selected != waveLimiterInfo.isSelected()) {
                    waveLimiterInfo.setSelected(selected);
                    fireEvent(WAVE_LIMITER);
                }
            }
        }
    }


    /**
     * Determine is mission has particular waveType based on what is in the waveLimiterInfos Array
     * <p/>
     * Used by the waveLimiterInfos GUI to enable/disable the appropriate 'Select All' toggle buttons
     *
     * @param waveType
     * @return true if waveType in waveLimiterInfos, otherwise false
     */
    public boolean hasWaveType(WavelengthInfo.WaveType waveType) {

        for (WavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (waveLimiterInfo.isWaveType(waveType)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Determines if all wavelengths for a given wavelength type within the wavelength limiter array are selected
     * <p/>
     * This is used to determine whether the toggle button in the wavelength limiter GUI needs
     * to be in: 'Select All Infrared' mode, 'Deselect All Infrared' mode,
     * 'Select All Visible' mode, or 'Deselect All Visible' mode
     *
     * @return true if all of given wavelength type selected, otherwise false
     */
    public boolean isSelectedAllWaveLimiter(WavelengthInfo.WaveType waveType) {

        int selectedCount = 0;

        for (WavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (waveLimiterInfo.isWaveType(waveType)) {
                if (waveLimiterInfo.isSelected()) {
                    selectedCount++;
                } else {
                    return false;
                }
            }
        }

        if (selectedCount > 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Sets all wavelengths of a given wavelength type within the wavelength limiter array to selected
     * <p/>
     * This is called by the wavelength limiter GUI toggle buttons and is also used for initializing defaults.
     *
     * @param selected
     */
    public void setSelectedAllWaveLimiter(WavelengthInfo.WaveType waveType, boolean selected) {

        for (WavelengthInfo waveLimiterInfo : waveLimiterInfos) {
            if (waveLimiterInfo.isWaveType(waveType)) {
                waveLimiterInfo.setSelected(selected);
            }
        }
        fireEvent(WAVE_LIMITER);
    }

    public void addParamInfo(ParamInfo paramInfo) {
        paramInfos.add(paramInfo);
    }

    public void clearParamInfo() {
        paramInfos.clear();
    }

    public ArrayList<ParamInfo> getParamInfos() {
        return paramInfos;
    }


    public void clearParamInfos() {
        paramInfos.clear();
    }


    public void sortParamCategoryInfos() {
        Collections.sort(paramCategoryInfos);
    }


    public void sortParamInfos() {
        Collections.sort(paramInfos);
    }


    public ArrayList<WavelengthInfo> getWaveLimiterInfos() {
        return waveLimiterInfos;
    }


    /**
     * Handle cases where a change in one name should effect a change in name
     * <p/>
     * In this case specifically coordParams and pixelParams are mutually exclusive
     * so if a name in one group is being set to a non-default value, then set all
     * params in the other group to the defaults
     *
     * @param name
     */
    private void setConflictingParams(String name) {

        ParamInfo paramInfo = getParamInfo(name);
        if (paramInfo == null) {
            return;
        }

        // Only proceed if name is not equal to default
        if (paramInfo.getValue() == paramInfo.getDefaultValue()) {
            return;
        }

        // Set all params in the other group to the defaults
        final HashSet<String> coords = new HashSet<String>();
        coords.add(NORTH);
        coords.add(SOUTH);
        coords.add(EAST);
        coords.add(WEST);

        final HashSet<String> pixels = new HashSet<String>();
        pixels.add(SPIXL);
        pixels.add(EPIXL);
        pixels.add(SLINE);
        pixels.add(ELINE);

        // Test if name is coordParam
        if (coords.contains(name)) {
            for (String pixelParam : pixels) {
                setParamToDefaults(getParamInfo(pixelParam));
            }
        }

        // Set all pixelParams in paramInfos to defaults
        if (pixels.contains(name)) {
            for (String coordParam : coords) {
                setParamToDefaults(getParamInfo(coordParam));
            }
        }
    }


    public String getParString() {
        return getParString(isShowDefaultsInParString());
    }

    public String getParString(boolean showDefaults) {

        StringBuilder par = new StringBuilder("");

        for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
            StringBuilder currCategoryEntries = new StringBuilder("");

            for (ParamInfo paramInfo : paramCategoryInfo.getParamInfos()) {
                if (paramInfo.getName().equals(IFILE)) {
                    currCategoryEntries.append(makeParEntry(paramInfo));
                } else if (paramInfo.getName().equals(OFILE)) {
                    currCategoryEntries.append(makeParEntry(paramInfo));
                } else if (paramInfo.getName().equals(GEOFILE)) {
                    if (isRequiresGeofile()) {
                        currCategoryEntries.append(makeParEntry(paramInfo));
                    }
                } else if (paramInfo.getName().equals(PAR)) {
                    // right ignore and do not print todo
                } else {
                    if (!paramInfo.getName().startsWith("-")) {

                        if (paramInfo.getValue().equals(paramInfo.getDefaultValue())) {
                            if (showDefaults) {
                                currCategoryEntries.append(makeParEntry(paramInfo, true));
                            }
                        } else {
                            currCategoryEntries.append(makeParEntry(paramInfo));
                        }
                    }
                }
            }

            if (currCategoryEntries.toString().length() > 0) {
                par.append("# " + paramCategoryInfo.getName().toUpperCase() + "\n");
                par.append(currCategoryEntries.toString());
                par.append("\n");
            }
        }

        return par.toString();
    }

    private String makeParEntry(ParamInfo paramInfo) {
        return makeParEntry(paramInfo, false);
    }

    private String makeParEntry(ParamInfo paramInfo, boolean commented) {
        StringBuilder line = new StringBuilder();

        if (paramInfo.getValue().length() > 0) {
            if (commented) {
                line.append("# ");
            }


            if (paramInfo.getType() == ParamInfo.Type.IFILE) {
                if (iFileInfo != null && !paramInfo.isValid(iFileInfo.getParentFile())) {
                    line.append("# WARNING!!! file " + paramInfo.getValue() + " does not exist" + "\n");
                } else if (paramInfo.getName().equals(IFILE)) {
                    if (!isValidIfile()) {
                        line.append("# WARNING!!! file " + paramInfo.getValue() + " is not a valid input file" + "\n");
                    }
                } else if (paramInfo.getName().equals(GEOFILE)) {
                    FileInfo geoFileInfo = getParamFileInfo(GEOFILE);
                    if (!geoFileInfo.isTypeId(FileTypeInfo.Id.GEO)) {
                        line.append("# WARNING!!! file " + paramInfo.getValue() + " is not a GEO file" + "\n");
                    }
                }
            }

            line.append(paramInfo.getName() + "=" + paramInfo.getValue() + "\n");
        }

        return line.toString();
    }


    private ArrayList<ParamInfo> parseParString(String parfileContents) {

        ArrayList<ParamInfo> paramInfos = new ArrayList<ParamInfo>();

        if (parfileContents != null) {

            String parfileLines[] = parfileContents.split("\n");

            for (String parfileLine : parfileLines) {

                // skip the comment lines in file
                if (!parfileLine.trim().startsWith("#")) {

                    String splitLine[] = parfileLine.split("=");
                    if (splitLine.length == 1 || splitLine.length == 2) {
                        String name = splitLine[0].toString().trim();
                        String value = null;

                        if (splitLine.length == 2) {
                            value = splitLine[1].toString().trim();
                        } else if (splitLine.length == 1) {
                            value = ParamInfo.NULL_STRING;
                        }

                        ParamInfo paramInfo = new ParamInfo(name, value);
                        paramInfos.add(paramInfo);
                    }
                }
            }
        }

        return paramInfos;
    }


    public void setParString(String parString, boolean ignoreIfile) {
        setParString(parString, ignoreIfile, false);
    }

    public void setParString(String parString, boolean ignoreIfile, boolean addParamsMode) {

        disableEvent(PARSTRING);
        ArrayList<ParamInfo> parfileParamInfos = parseParString(parString);

        /*
        Handle IFILE first
         */
        if (!ignoreIfile) {
            for (ParamInfo parfileParamInfo : parfileParamInfos) {
                if (parfileParamInfo.getName().toLowerCase().equals(IFILE)) {
                    setParamValue(getIfileParamInfo(), parfileParamInfo.getValue());
                    break;
                }
            }
        }
        /*
        Set all params contained in parString
        Ignore IFILE (handled earlier) and PAR (which is todo)
         */
        for (ParamInfo newParamInfo : parfileParamInfos) {


            if (newParamInfo.getName().toLowerCase().equals(OFILE) && ignoreIfile) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().equals(GEOFILE) && ignoreIfile) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().equals(IFILE)) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().equals(PAR)) {
                continue;
            }

            if (newParamInfo.getName().toLowerCase().equals(L2PROD)) {
                newParamInfo.setValue(sortStringList(newParamInfo.getValue()));
            }

            setParamValue(newParamInfo.getName(), newParamInfo.getValue());
        }


        if (!addParamsMode) {

            /*
           Delete all params NOT contained in parString to defaults (basically set to default)
           Except: L2PROD and IFILE  remain at current value
            */
            for (ParamInfo paramInfo : paramInfos) {
                if (!paramInfo.getName().equals(L2PROD) && !paramInfo.getName().equals(IFILE) && !paramInfo.getName().equals(OFILE) && !paramInfo.getName().equals(GEOFILE)) {
                    boolean paramHandled = false;
                    for (ParamInfo parfileParamInfo : parfileParamInfos) {
                        if (paramInfo.getName().toLowerCase().equals(parfileParamInfo.getName().toLowerCase())) {
                            paramHandled = true;
                        }
                    }

                    if (!paramHandled && (paramInfo.getValue() != paramInfo.getDefaultValue())) {
                        setParamValue(paramInfo.getName(), paramInfo.getDefaultValue());
                    }
                }
            }

        }

        fireEvent(PARSTRING);
        enableEvent(PARSTRING);
    }


    public ParamInfo getParamInfo(String name) {

        if (name == null) {
            return null;
        }

        name = name.trim().toLowerCase();

        if (paramInfoHashMap.size() > 0) {
            if (paramInfoHashMap.containsKey(name)) {
                return paramInfoHashMap.get(name);
            }
        } else {
            for (ParamInfo paramInfo : paramInfos) {
                if (paramInfo.getName().toLowerCase().equals(name)) {
                    return paramInfo;
                }
            }
        }

        return null;
    }


    private String getParamValue(ParamInfo paramInfo) {
        return paramInfo.getValue();
    }


    public String getParamValue(String name) {
        return getParamValue(getParamInfo(name));
    }


    private boolean getBooleanParamValue(ParamInfo paramInfo) {
        if (paramInfo.getValue().equals(ParamInfo.BOOLEAN_TRUE)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean getBooleanParamValue(String name) {
        return getBooleanParamValue(getParamInfo(name));
    }


    private boolean isValidParamValue(ParamInfo paramInfo) {
        if (paramInfo != null && iFileInfo != null) {
            return paramInfo.isValid(iFileInfo.getParentFile());
        }

        return false;
    }

    public boolean isValidParamValue(String name) {
        return isValidParamValue(getParamInfo(name));
    }


    private FileInfo getParamFileInfo(ParamInfo paramInfo) {
        if (paramInfo != null && iFileInfo != null) {
            return paramInfo.getFileInfo(iFileInfo.getParentFile());
        }
        return null;
    }

    public FileInfo getParamFileInfo(String name) {
        return getParamFileInfo(getParamInfo(name));
    }


    public void setParamValueAndDefault(String name, String value) {
        setParamValueAndDefault(getParamInfo(name), value);
    }


    public void setParamValueAndDefault(ParamInfo paramInfo, String value) {
        if (paramInfo == null) {
            return;
        }
        if (value == null) {
            value = ParamInfo.NULL_STRING;
        }

        if (!value.equals(paramInfo.getValue()) || !value.equals(paramInfo.getDefaultValue())) {
            if (paramInfo.getName().toLowerCase().equals(IFILE)) {
                setIfileParamValue(paramInfo, value);
                paramInfo.setDefaultValue(paramInfo.getValue());
            } else {
                paramInfo.setValue(value);
                paramInfo.setDefaultValue(paramInfo.getValue());
                setConflictingParams(paramInfo.getName());
                fireEvent(paramInfo.getName());
            }
        }
    }


    private void setParamValue(ParamInfo paramInfo, String value) {
        if (paramInfo == null) {
            return;
        }
        if (value == null) {
            value = ParamInfo.NULL_STRING;
        }

        if (!value.equals(paramInfo.getValue())) {
            if (paramInfo.getName().toLowerCase().equals(IFILE)) {
                setIfileParamValue(paramInfo, value);
            } else {
                if (value.length() > 0 || paramInfo.getName().toLowerCase().equals(L2PROD)) {
                    paramInfo.setValue(value);
                    setConflictingParams(paramInfo.getName());
                } else {
                    paramInfo.setValue(paramInfo.getDefaultValue());
                }
                fireEvent(paramInfo.getName());
            }

        }
    }


    public void setParamValue(String name, String value) {
        setParamValue(getParamInfo(name), value);
    }


    private void setParamValue(ParamInfo paramInfo, boolean selected) {
        if (selected) {
            setParamValue(paramInfo, ParamInfo.BOOLEAN_TRUE);
        } else {
            setParamValue(paramInfo, ParamInfo.BOOLEAN_FALSE);
        }
    }

    public void setParamValue(String name, boolean selected) {
        setParamValue(getParamInfo(name), selected);
    }


    private void setParamValue(ParamInfo paramInfo, ParamValidValueInfo paramValidValueInfo) {
        setParamValue(paramInfo, paramValidValueInfo.getValue());
    }


    public void setParamValue(String name, ParamValidValueInfo paramValidValueInfo) {
        setParamValue(getParamInfo(name), paramValidValueInfo);
    }

    private boolean isParamDefault(ParamInfo paramInfo) {
        if (paramInfo.getValue().equals(paramInfo.getDefaultValue())) {
            return true;
        } else {
            return false;
        }
    }


    public boolean isParamDefault(String name) {
        return isParamDefault(getParamInfo(name));
    }


    private String getParamDefault(ParamInfo paramInfo) {
        if (paramInfo != null) {
            return paramInfo.getDefaultValue();
        } else {
            return null;
        }
    }

    public String getParamDefault(String name) {
        return getParamDefault(getParamInfo(name));
    }


    private void setParamToDefaults(ParamInfo paramInfo) {
        if (paramInfo != null) {
            setParamValue(paramInfo, paramInfo.getDefaultValue());
        }
    }


    public void setParamToDefaults(String name) {
        setParamToDefaults(getParamInfo(name));
    }

    public void setToDefaults(L2genParamCategoryInfo paramCategoryInfo) {
        for (ParamInfo paramInfo : paramCategoryInfo.getParamInfos()) {
            setParamToDefaults(paramInfo);
        }
    }


    public boolean isParamCategoryDefault(L2genParamCategoryInfo paramCategoryInfo) {
        boolean isDefault = true;

        for (ParamInfo paramInfo : paramCategoryInfo.getParamInfos()) {
            if (!paramInfo.isDefault()) {
                isDefault = false;
            }
        }

        return isDefault;
    }


    private String getSensorInfoFilename() {

        if (iFileInfo != null && iFileInfo.getMissionDirectory() != null) {
            // determine the filename which contains the wavelengths
            final StringBuilder sensorInfoFilename = new StringBuilder("");
            sensorInfoFilename.append(iFileInfo.getMissionDirectory());
            sensorInfoFilename.append("/");
            sensorInfoFilename.append("msl12_sensor_info.dat");
            return sensorInfoFilename.toString();
        } else {
            return null;
        }
    }


    private void resetWaveLimiter() {
        waveLimiterInfos.clear();

        // determine the filename which contains the wavelengths
        String sensorInfoFilename = getSensorInfoFilename();

        if (sensorInfoFilename != null) {
            // read in the mission's datafile which contains the wavelengths
            //  final ArrayList<String> SensorInfoArrayList = myReadDataFile(sensorInfoFilename.toString());
            final ArrayList<String> SensorInfoArrayList = l2genReader.readFileIntoArrayList(sensorInfoFilename);
            debug("sensorInfoFilename=" + sensorInfoFilename);


            // loop through datafile
            for (String myLine : SensorInfoArrayList) {

                // skip the comment lines in file
                if (!myLine.trim().startsWith("#")) {

                    // just look at value pairs of the form Lambda(#) = #
                    String splitLine[] = myLine.split("=");
                    if (splitLine.length == 2 &&
                            splitLine[0].trim().startsWith("Lambda(") &&
                            splitLine[0].trim().endsWith(")")
                            ) {

                        // get current wavelength and add into in a JCheckBox
                        final String currWavelength = splitLine[1].trim();

                        WavelengthInfo wavelengthInfo = new WavelengthInfo(currWavelength);
                        waveLimiterInfos.add(wavelengthInfo);
                        debug("wavelengthLimiterArray adding wave=" + wavelengthInfo.getWavelengthString());
                    }
                }
            }
        }
    }


    // runs this if IFILE changes
    // it will reset missionString
    // it will reset and make new wavelengthInfoArray
    private void setIfileParamValue(ParamInfo paramInfo, String newIfile) {

        disableEvent(PARSTRING);
        disableEvent(L2PROD);

        String oldIfile = getParamValue(getIfileParamInfo());

        if (newIfile != null && newIfile.length() > 0) {
            iFileInfo = new FileInfo(newIfile);
        } else {
            iFileInfo = null;
        }

        paramInfo.setValue(newIfile);
        paramInfo.setDefaultValue(newIfile);

        if (iFileInfo != null && isValidIfile()) {
            resetWaveLimiter();
            l2prodParamInfo.resetProductInfos();
            updateXmlBasedObjects((File) iFileInfo);

            FileInfo oFileInfo = FilenamePatterns.getOFileInfo(iFileInfo);
            if (oFileInfo != null) {
                setParamValueAndDefault(OFILE, oFileInfo.getAbsolutePath());
            }

            if (iFileInfo.isGeofileRequired()) {
                FileInfo geoFileInfo = FilenamePatterns.getGeoFileInfo(iFileInfo);
                if (geoFileInfo != null) {
                    setParamValueAndDefault(GEOFILE, geoFileInfo.getAbsolutePath());
                }
            } else {
                setParamValueAndDefault(GEOFILE, null);
            }
        } else {
            setParamValueAndDefault(OFILE, null);
            setParamValueAndDefault(GEOFILE, null);
            fireEvent(INVALID_IFILE);
        }

        setParamValueAndDefault(PAR, ParamInfo.NULL_STRING);


        fireEvent(IFILE, oldIfile, newIfile);

        fireEvent(PARSTRING);
        enableEvent(L2PROD);
        enableEvent(PARSTRING);

    }


    public void setAncillaryFiles() {

        if (!isValidIfile()) {
            System.out.println("ERROR - Can not run getanc.py without a valid ifile.");
            return;
        }

        // get the ifile
        String ifile = getParamValue(getIfileParamInfo());
        StringBuilder ancillaryFiles = new StringBuilder("");

        ProcessorModel processorModel = new ProcessorModel("getanc.py");
        processorModel.setAcceptsParFile(false);
        processorModel.addParamInfo("ifile", ifile, 1);

        try {
            Process p = processorModel.executeProcess();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = stdInput.readLine();
            while (line != null) {
                if (line.contains("=")) {
                    ancillaryFiles.append(line);
                    ancillaryFiles.append("\n");
                }
                line = stdInput.readLine();
            }
        } catch (IOException e) {
            System.out.println("ERROR - Problem running getanc.py");
            System.out.println(e.getMessage());
            return;
        }

        setParString(ancillaryFiles.toString(), true, true);
    }


    private void debug(String string) {

        //  System.out.println(string);
    }


    /**
     * resets paramInfos within paramCategoryInfos to link to appropriate entry in paramInfos
     */
    public void setParamCategoryInfos() {
        for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
            paramCategoryInfo.clearParamInfos();
        }

        for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
            for (String categorizedParamName : paramCategoryInfo.getParamNames()) {
                for (ParamInfo paramInfo : paramInfos) {
                    if (categorizedParamName.equals(paramInfo.getName())) {
                        paramCategoryInfo.addParamInfos(paramInfo);
                    }
                }
            }
        }


        for (ParamInfo paramInfo : paramInfos) {
            boolean found = false;

            for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
                for (String categorizedParamName : paramCategoryInfo.getParamNames()) {
                    if (categorizedParamName.equals(paramInfo.getName())) {
                        //  paramCategoryInfo.addParamInfos(paramInfo);
                        found = true;
                    }
                }
            }

            if (!found) {
                for (L2genParamCategoryInfo paramCategoryInfo : paramCategoryInfos) {
                    if (paramCategoryInfo.isDefaultBucket()) {
                        paramCategoryInfo.addParamInfos(paramInfo);
                        l2genPrint.adminlog("Dropping uncategorized param '" + paramInfo.getName() + "' into the defaultBucket");
                    }
                }
            }
        }


    }


    public boolean compareWavelengthLimiter(WavelengthInfo wavelengthInfo) {
        for (WavelengthInfo waveLimitorInfo : getWaveLimiterInfos()) {
            if (waveLimitorInfo.getWavelength() == wavelengthInfo.getWavelength()) {
                if (waveLimitorInfo.isSelected()) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    public ArrayList<L2genParamCategoryInfo> getParamCategoryInfos() {
        return paramCategoryInfos;
    }


    public void addParamCategoryInfo(L2genParamCategoryInfo paramCategoryInfo) {
        paramCategoryInfos.add(paramCategoryInfo);
    }

    public void clearParamCategoryInfos() {
        paramCategoryInfos.clear();
    }


    private void updateXmlBasedObjects(File iFile) {

        InputStream paramInfoStream = getParamInfoInputStream(iFile);

        l2genReader.updateParamInfosWithXml(paramInfoStream);
    }


    private InputStream getProductInfoInputStream(File file) {

        String paramInfoXml = PRODUCT_INFO_XML;

        return L2genForm.class.getResourceAsStream(paramInfoXml);
    }


    private InputStream getParamInfoInputStream(File file) {

//        // get the ifile
//        String ifile = file.getAbsolutePath();
//        File workDir = file.getParentFile();
//
//        ProcessorModel processorModel = new ProcessorModel("l2gen");
//        processorModel.setAcceptsParFile(true);
//        processorModel.setInputFile(file);
//        processorModel.setOutputFileDir(workDir);
//
//        processorModel.addParamInfo("ifile", ifile);
//        processorModel.addParamInfo("ofile", ifile);
//
//
//
//        try {
//            Process p = processorModel.executeProcess();
//            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//            String line = stdInput.readLine();
//            while (line != null) {
//                if (line.contains("=")) {
//                    ancillaryFiles.append(line);
//                    ancillaryFiles.append("\n");
//                }
//                line = stdInput.readLine();
//            }
//        } catch (IOException e) {
//            System.out.println("ERROR - Problem running getanc.py");
//            System.out.println(e.getMessage());
//            return;
//        }
//


        String paramInfoXml = PARAM_INFO_XML;

        return L2genForm.class.getResourceAsStream(paramInfoXml);
    }


    public void setInitialValues(File iFile) {
        if (iFile != null) {
            setParamValueAndDefault(IFILE, iFile.toString());
        } else {
            setParamValueAndDefault(IFILE, ParamInfo.NULL_STRING);
        }
    }

    public boolean initXmlBasedObjects() {
//todo
        if (1 == 1 || DEFAULT_IFILE.exists()) {
            InputStream productInfoStream = getProductInfoInputStream(DEFAULT_IFILE);
            InputStream paramInfoStream = getParamInfoInputStream(DEFAULT_IFILE);

            if (paramInfoStream != null && productInfoStream != null) {
                disableEvent(PARSTRING);
                disableEvent(L2PROD);
                //              fireEvent(PARSTRING_IN_PROGRESS_EVENT);
                l2genReader.readParamInfoXml(paramInfoStream, productInfoStream);

                InputStream paramCategoryInfoStream = L2genForm.class.getResourceAsStream(PARAM_CATEGORY_INFO_XML);
                l2genReader.readParamCategoryXml(paramCategoryInfoStream);
                setParamCategoryInfos();

                // set the useful paramInfo shortcuts
                initParamInfoHashMap();
                setIfileParamInfo(getParamInfo(IFILE));
                setL2prodParamInfo((L2genProductsParamInfo) getParamInfo(L2PROD));
                setOfileParamInfo(getParamInfo(OFILE));


                fireEvent(PARSTRING);
                enableEvent(L2PROD);
                enableEvent(PARSTRING);

                return true;
            }
        }

        return false;
    }


    public void setL2prodParamInfo(L2genProductsParamInfo l2prodParamInfo) {
        this.l2prodParamInfo = l2prodParamInfo;
    }


    public void addProductInfo(ProductInfo productInfo) {
        l2prodParamInfo.addProductInfo(productInfo);
    }


    public void clearProductInfos() {
        l2prodParamInfo.clearProductInfos();
    }


    public void sortProductInfos(Comparator<ProductInfo> comparator) {
        l2prodParamInfo.sortProductInfos(comparator);
    }

    public void setProdToDefault() {
        if (!l2prodParamInfo.isDefault()) {
            l2prodParamInfo.setValue(l2prodParamInfo.getDefaultValue());
            fireEvent(L2PROD);
        }
    }


    /**
     * resets productInfos within productCategoryInfos to link to appropriate entry in productInfos
     */
    public void setProductCategoryInfos() {
        l2prodParamInfo.setProductCategoryInfos();
    }

    public ArrayList<ProductCategoryInfo> getProductCategoryInfos() {
        return l2prodParamInfo.getProductCategoryInfos();
    }

    public void addProductCategoryInfo(ProductCategoryInfo productCategoryInfo) {
        l2prodParamInfo.addProductCategoryInfo(productCategoryInfo);
    }

    public void clearProductCategoryInfos() {
        l2prodParamInfo.clearProductCategoryInfos();
    }

    public L2genProductsParamInfo createL2prodParamInfo(String value, InputStream productInfoStream) {
        L2genProductsParamInfo l2prodParamInfo = new L2genProductsParamInfo();
        setL2prodParamInfo(l2prodParamInfo);

        l2genReader.readProductsXml(productInfoStream);

        l2prodParamInfo.setValue(value);

        InputStream productCategoryInfoStream = L2genForm.class.getResourceAsStream(PRODUCT_CATEGORY_INFO_XML);
        l2genReader.readProductCategoryXml(productCategoryInfoStream);
        setProductCategoryInfos();

        return l2prodParamInfo;
    }

    public String sortStringList(String stringlist) {
        String[] products = stringlist.split("\\s+");
        ArrayList<String> productArrayList = new ArrayList<String>();
        for (String product : products) {
            productArrayList.add(product);
        }
        Collections.sort(productArrayList);

        return StringUtils.join(productArrayList, " ");
    }
}


