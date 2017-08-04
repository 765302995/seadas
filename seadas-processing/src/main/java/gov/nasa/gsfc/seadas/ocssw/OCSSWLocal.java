package gov.nasa.gsfc.seadas.ocssw;

import com.bc.ceres.core.runtime.RuntimeContext;
import gov.nasa.gsfc.seadas.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.common.MissionInfo;
import gov.nasa.gsfc.seadas.processing.common.ParFileManager;
import gov.nasa.gsfc.seadas.processing.common.SeadasFileUtils;
import gov.nasa.gsfc.seadas.processing.core.ParamInfo;
import gov.nasa.gsfc.seadas.processing.core.ParamList;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.processor.MultlevelProcessorForm;
import gov.nasa.gsfc.seadas.processing.utilities.SeadasArrayUtils;
import org.apache.commons.lang.ArrayUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import static java.lang.System.getProperty;

/**
 * Created by aabduraz on 3/27/17.
 */
public class OCSSWLocal extends OCSSW {

    private static String NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME = "next_level_name.py";
    private static String NEXT_LEVEL_FILE_NAME_TOKEN = "Output Name:";
    public static final String GET_OBPG_FILE_TYPE_PROGRAM_NAME = "get_obpg_file_type.py";
    public static String OCSSW_SCRIPTS_DIR_SUFFIX = "run" + System.getProperty("file.separator") + "scripts";
    public static String OCSSW_DATA_DIR_SUFFIX = "run" + System.getProperty("file.separator") + "data";


    public static String TMP_OCSSW_INSTALLER_PROGRAM_PATH = (new File(System.getProperty("java.io.tmpdir"), "install_ocssw.py")).getPath();

    private static final String DEFAULTS_FILE_PREFIX = "msl12_defaults_",
            AQUARIUS_DEFAULTS_FILE_PREFIX = "l2gen_aquarius_defaults_",
            L3GEN_DEFAULTS_FILE_PREFIX = "msl12_defaults_";

    private String defaultsFilePrefix;

    private final static String L2GEN_PROGRAM_NAME = "l2gen",
            AQUARIUS_PROGRAM_NAME = "l2gen_aquarius",
            L3GEN_PROGRAM_NAME = "l3gen";

    public OCSSWLocal() {

        initiliaze();
        ocsswExist = isOCSSWExist();

    }

    private void initiliaze() {
        String dirPath = RuntimeContext.getConfig().getContextProperty(OCSSWROOT_PROPERTY, System.getenv(OCSSWROOT_ENVVAR));

        if (dirPath == null) {
            dirPath = RuntimeContext.getConfig().getContextProperty(SEADASHOME_PROPERTY, System.getProperty("user.home") + System.getProperty("file.separator") + "ocssw");
        }
        if (dirPath != null) {
            final File dir = new File(dirPath + System.getProperty("file.separator") + OCSSW_SCRIPTS_DIR_SUFFIX);
            if (dir.isDirectory()) {
                ocsswExist = true;
                ocsswRoot = dirPath;
                ocsswScriptsDirPath = ocsswRoot + System.getProperty("file.separator") + OCSSW_SCRIPTS_DIR_SUFFIX;
                ocsswDataDirPath = ocsswRoot + System.getProperty("file.separator") + OCSSW_DATA_DIR_SUFFIX;
                ocsswInstallerScriptPath = ocsswScriptsDirPath + System.getProperty("file.separator") + OCSSW_INSTALLER_PROGRAM;
                ocsswRunnerScriptPath = ocsswScriptsDirPath + System.getProperty("file.separator") + OCSSW_RUNNER_SCRIPT;
                initiliazeMissions();
            }
        }
    }


    private void initiliazeMissions() {
        missionInfo = new MissionInfo();
    }


    public boolean isMissionDirExist(String missionName) {
        String missionDir = ocsswDataDirPath + File.separator + missionInfo.getDirectory();
        return new File(missionDir).exists();
    }


    @Override
    public Process execute(ProcessorModel processorModel) {
        setProgramName(processorModel.getProgramName());
        return execute(getProgramCommandArray(processorModel));
    }

    @Override
    public Process execute(ParamList paramListl) {
        String[] programNameArray = {programName};
        commandArray = SeadasArrayUtils.concatAll(commandArrayPrefix, programNameArray, getCommandArrayParam(paramListl), commandArraySuffix);
        return execute(commandArray);
    }

    /**
     * this method returns a command array for execution.
     * the array is constructed using the paramList data and input/output files.
     * the command array structure is: full pathname of the program to be executed, input file name, params in the required order and finally the output file name.
     * assumption: order starts with 1
     *
     * @return
     */
    public String[] getProgramCommandArray(ProcessorModel processorModel) {

        String[] cmdArray;
        String[] programNameArray = {programName};
        String[] cmdArrayForParams;

        ParFileManager parFileManager = new ParFileManager(processorModel);

        if (processorModel.acceptsParFile()) {
            cmdArrayForParams = parFileManager.getCmdArrayWithParFile();

        } else {
            cmdArrayForParams = getCommandArrayParam(processorModel.getParamList());
        }

        //The final command array is the concatination of commandArrayPrefix, cmdArrayForParams, and commandArraySuffix
        cmdArray = SeadasArrayUtils.concatAll(commandArrayPrefix, programNameArray, cmdArrayForParams, commandArraySuffix);

        // get rid of the null strings
        ArrayList<String> cmdList = new ArrayList<String>();
        for (String s : cmdArray) {
            if (s != null) {
                cmdList.add(s);
            }
        }
        cmdArray = cmdList.toArray(new String[cmdList.size()]);

        return cmdArray;
    }

    @Override
    public Process execute(String[] commandArray) {

        ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
//
//        String ifileDir = getIfileName().substring(0, getIfileName().lastIndexOf(System.getProperty("file.separator")));
//
//        processBuilder.directory(new File(ifileDir));

        Process process = null;
        try {
            process = processBuilder.start();
            if (process != null) {
                SeadasFileUtils.debug("Running the program " + commandArray.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return process;
    }

    @Override
    public Process execute(String programName, String[] commandArrayParams) {
        return null;
    }

    @Override
    public String getOfileName(String ifileName) {

        this.setIfileName(ifileName);
        extractFileInfo(ifileName);
        if (programName.equals("extractor")) {
            selectExtractorProgram();
        }

        if (ifileName == null || programName == null) {
            return null;
        }
        if (programName.equals("l3bindump")) {
            return ifileName + ".xml";
        } else if (programName.equals("extractor")) {

        }

        String[] commandArrayParams = {NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME, ifileName, programName};

        return getOfileName(SeadasArrayUtils.concatAll(commandArrayPrefix, commandArrayParams));


    }

    public String[] getMissionSuites(String missionName, String programName) {
        MissionInfo missionInfo = new MissionInfo();

        missionInfo.setName(missionName);

        File missionDir = new File(getOcsswDataDirPath() + File.separator + missionInfo.getDirectory());

        System.out.println("mission directory: " + missionDir);

        if (missionDir.exists()) {

            ArrayList<String> suitesArrayList = new ArrayList<String>();

            File missionDirectoryFiles[] = missionDir.listFiles();

            for (File file : missionDirectoryFiles) {
                String filename = file.getName();

                if (filename.startsWith(getDefaultsFilePrefix(programName)) && filename.endsWith(".par")) {
                    String filenameTrimmed = filename.replaceFirst(getDefaultsFilePrefix(programName), "");
                    filenameTrimmed = filenameTrimmed.replaceAll("[\\.][p][a][r]$", "");
                    suitesArrayList.add(filenameTrimmed);
                }
            }

            final String[] suitesArray = new String[suitesArrayList.size()];

            int i = 0;
            for (String suite : suitesArrayList) {
                suitesArray[i] = suite;
                i++;
            }

            java.util.Arrays.sort(suitesArray);

            return suitesArray;

        } else {
            return null;
        }
    }

    public String getDefaultsFilePrefix(String programName) {

        defaultsFilePrefix = DEFAULTS_FILE_PREFIX;

        if (programName.equals(L3GEN_PROGRAM_NAME)) {
            defaultsFilePrefix = L3GEN_DEFAULTS_FILE_PREFIX;
        } else if (programName.equals(AQUARIUS_PROGRAM_NAME)) {
            defaultsFilePrefix = AQUARIUS_DEFAULTS_FILE_PREFIX;
        }
        return defaultsFilePrefix;
    }

    private void extractFileInfo(String ifileName) {

        String[] fileTypeCommandArrayParams = {GET_OBPG_FILE_TYPE_PROGRAM_NAME, ifileName};

        Process process = execute((String[]) ArrayUtils.addAll(commandArrayPrefix, fileTypeCommandArrayParams));

        try {

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = stdInput.readLine();
            if (line != null) {
                String splitLine[] = line.split(":");
                if (splitLine.length == 3) {
                    String missionName = splitLine[1].toString().trim();
                    String fileType = splitLine[2].toString().trim();

                    if (fileType.length() > 0) {
                        setFileType(fileType);
                    }

                    if (missionName.length() > 0) {
                        setMissionName(missionName);
                    }
                }
            }
        } catch (IOException ioe) {

            VisatApp.getApp().showErrorDialog(ioe.getMessage());
        }
    }

    @Override
    public String getOfileName(String ifileName, String[] options) {
        if (ifileName == null || programName == null) {
            return null;
        }
        if (programName.equals("l3bindump")) {
            return ifileName + ".xml";
        }

        String[] commandArrayParams = {NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME, ifileName, programName};

        return getOfileName(SeadasArrayUtils.concatAll(commandArrayPrefix, commandArrayParams, options));
    }

    @Override
    public String getOfileName(String ifileName, String programName, String suiteValue) {
        this.programName = programName;
        String[] additionalOptions = {"--suite=" + suiteValue};
        return getOfileName(ifileName, additionalOptions);
    }


    private String getOfileName(String[] commandArray) {

        Process process = execute(commandArray);

        if (process == null) {
            return null;
        }

        //wait for process to exit
        try {
            Field field = process.getClass().getDeclaredField("hasExited");
            field.setAccessible(true);
            while (!(Boolean) field.get(process)) {
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        int exitCode = process.exitValue();
        InputStream is;
        if (exitCode == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }


        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        try {

            if (exitCode == 0) {
                String line = br.readLine();
                while (line != null) {
                    if (line.startsWith(NEXT_LEVEL_FILE_NAME_TOKEN)) {
                        return (line.substring(NEXT_LEVEL_FILE_NAME_TOKEN.length())).trim();
                    }
                    line = br.readLine();
                }

            } else {
                Debug.trace("Failed exit code on program '" + NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME + "'");
            }

        } catch (IOException ioe) {

            VisatApp.getApp().showErrorDialog(ioe.getMessage());
        }

        return null;
    }

    public void setCommandArrayPrefix() {

        if (programName.equals(OCSSW_INSTALLER_PROGRAM)) {
            commandArrayPrefix = new String[1];
            commandArrayPrefix[0] = programName;
            if (!isOCSSWExist()) {
                commandArrayPrefix[0] = TMP_OCSSW_INSTALLER_PROGRAM_PATH;
            } else {
                commandArrayPrefix[0] = getOcsswInstallerScriptPath();
            }
        } else {
            commandArrayPrefix = new String[3];
            commandArrayPrefix[0] = ocsswRunnerScriptPath;
            commandArrayPrefix[1] = "--ocsswroot";
            commandArrayPrefix[2] = ocsswRoot;
        }
    }


    private String[] getCommandArrayParam(ParamList paramList) {

        ArrayList<String> commandArrayList = new ArrayList();

        Iterator itr = paramList.getParamArray().iterator();

        ParamInfo option;
        int optionOrder;
        String optionValue;
        int i = 0;
        while (itr.hasNext()) {
            option = (ParamInfo) itr.next();
            optionOrder = option.getOrder();
            optionValue = option.getValue();
            if (option.getType() != ParamInfo.Type.HELP) {
                if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_ARGUMENT)) {
                    if (option.getValue() != null && option.getValue().length() > 0) {
                        commandArrayList.add(optionValue);
                    }
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_OPTION) && !option.getDefaultValue().equals(option.getValue())) {
                    commandArrayList.add(option.getName() + "=" + optionValue);
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_FLAG) && (option.getValue().equals("true") || option.getValue().equals("1"))) {
                    if (option.getName() != null && option.getName().length() > 0) {
                        commandArrayList.add(option.getName());
                    }
                }
            }
        }
        String[] commandArrayParam = new String[commandArrayList.size()];
        commandArrayParam = commandArrayList.toArray(commandArrayParam);
        return commandArrayParam;
    }

    @Override
    public void setCommandArraySuffix() {

    }

    @Override
    public void setOcsswDataDirPath(String ocsswDataDirPath) {

    }


    @Override
    public void setOcsswInstallDirPath(String ocsswInstallDirPath) {

    }


    @Override
    public void setOcsswScriptsDirPath(String ocsswScriptsDirPath) {

    }


    @Override
    public void setOcsswInstallerScriptPath(String ocsswInstallerScriptPath) {

    }
}
