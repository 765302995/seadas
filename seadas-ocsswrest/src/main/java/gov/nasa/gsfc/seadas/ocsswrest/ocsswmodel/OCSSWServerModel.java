package gov.nasa.gsfc.seadas.ocsswrest.ocsswmodel;

import gov.nasa.gsfc.seadas.ocsswrest.database.SQLiteJDBC;
import gov.nasa.gsfc.seadas.ocsswrest.utilities.ServerSideFileUtilities;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Created by aabduraz on 3/27/17.
 */
public class OCSSWServerModel {

    public static final String OS_64BIT_ARCHITECTURE = "_64";
    public static final String OS_32BIT_ARCHITECTURE = "_32";

    private static String NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME = "next_level_name.py";
    private static String NEXT_LEVEL_FILE_NAME_TOKEN = "Output Name:";
    public static final String GET_OBPG_FILE_TYPE_PROGRAM_NAME = "get_obpg_file_type.py";

    public static final String OCSSW_SCRIPTS_DIR_SUFFIX = "scripts";
    public static final String OCSSW_DATA_DIR_SUFFIX = "share";
    public static final String OCSSW_BIN_DIR_SUFFIX = "bin";

    public static String OCSSW_INSTALLER_PROGRAM = "install_ocssw.py";
    public static String OCSSW_RUNNER_SCRIPT = "ocssw_runner";

    public static String TMP_OCSSW_INSTALLER_PROGRAM_PATH = (new File(System.getProperty("java.io.tmpdir"), "install_ocssw.py")).getPath();


    public static String getOcsswDataDirPath() {
        return ocsswDataDirPath;
    }


    public static String getOcsswScriptsDirPath() {
        return ocsswScriptsDirPath;
    }

    public static String getOcsswInstallerScriptPath() {
        return ocsswInstallerScriptPath;
    }

    public static String getOcsswRunnerScriptPath() {
        return ocsswRunnerScriptPath;
    }

    public static String getOcsswBinDirPath() {
        return ocsswBinDirPath;
    }

    public enum ExtractorPrograms{
        L1AEXTRACT_MODIS("l1aextract_modis"),
        L1AEXTRACT_SEAWIFS("l1extract_seawifs"),
        L1AEXTRACT__VIIRS("l1aextract_viirs"),
        L2EXTRACT("l2extract");

        String extractorProgramName;

        ExtractorPrograms(String programName) {
            extractorProgramName = programName;
        }

        public String getExtractorProgramName(){
            return extractorProgramName;
        }
    }

    final String L1AEXTRACT_MODIS = "l1aextract_modis",
            L1AEXTRACT_MODIS_XML_FILE = "l1aextract_modis.xml",
            L1AEXTRACT_SEAWIFS = "l1aextract_seawifs",
            L1AEXTRACT_SEAWIFS_XML_FILE = "l1aextract_seawifs.xml",
            L1AEXTRACT_VIIRS = "l1aextract_viirs",
            L1AEXTRACT_VIIRS_XML_FILE = "l1aextract_viirs.xml",
            L2EXTRACT = "l2extract",
            L2EXTRACT_XML_FILE = "l2extract.xml";

    private static boolean ocsswExist;
    private static String ocsswRoot;
    private static String ocsswDataDirPath;
    private static String ocsswScriptsDirPath;
    private static String ocsswInstallerScriptPath;
    private static String ocsswRunnerScriptPath;
    private static String ocsswBinDirPath;


    static boolean isProgramValid;


    public OCSSWServerModel(){

        initiliaze();
        ocsswExist = isOCSSWExist();

    }

    public static void initiliaze(){
        String ocsswRootPath = System.getProperty("ocsswroot");
        if (ocsswRootPath != null) {
            final File dir = new File(ocsswRootPath + File.separator + OCSSW_SCRIPTS_DIR_SUFFIX);
            System.out.println("server ocssw root path: " + dir.getAbsoluteFile());
            if (dir.isDirectory()) {
                ocsswExist = true;
                ocsswRoot = ocsswRootPath;
                ocsswScriptsDirPath = ocsswRoot + File.separator + OCSSW_SCRIPTS_DIR_SUFFIX;
                ocsswDataDirPath = ocsswRoot + File.separator + OCSSW_DATA_DIR_SUFFIX;
                ocsswBinDirPath = ocsswRoot + File.separator + OCSSW_BIN_DIR_SUFFIX;
                ocsswInstallerScriptPath = ocsswScriptsDirPath + File.separator + OCSSW_INSTALLER_PROGRAM;
                ocsswRunnerScriptPath = ocsswScriptsDirPath + File.separator + OCSSW_RUNNER_SCRIPT;
            }
        }
    }


    public static boolean isMissionDirExist(String missionName) {
        missionName = SQLiteJDBC.retrieveMissionDir(missionName);
        System.out.println("mission dir = " +  ocsswDataDirPath + File.separator + missionName);
        return new File(ocsswDataDirPath + File.separator + missionName).exists();
    }

    public static boolean isOCSSWExist(){
        return ocsswExist;
    }

    public static String getOcsswRoot(){
        return ocsswRoot;
    }



    /**
     * This method will validate the program name. Only programs exist in $OCSSWROOT/run/scripts and $OSSWROOT/run/bin/"os_name" can be executed on the server side.
     * @param programName
     * @return true if programName is found in the $OCSSWROOT/run/scripts or $OSSWROOT/run/bin/"os_name" directories. Otherwise return false.
     */
    public static boolean isProgramValid(String programName) {
        //"extractor" is special, it can't be validated using the same logic for other programs.
        if (programName.equals("extractor")) {
            return true;
        }
        isProgramValid = false;
        File scriptsFolder = new File(ocsswScriptsDirPath);
        File[] listOfScripts = scriptsFolder.listFiles();
        File runFolder = new File(ocsswBinDirPath);
        File[] listOfPrograms = runFolder.listFiles();

        File[] executablePrograms = ServerSideFileUtilities.concatAll(listOfPrograms, listOfScripts);

        for (File file:executablePrograms) {
            if (file.isFile() && programName.equals(file.getName())) {
                isProgramValid = true;
                break;
            }
        }
        return isProgramValid;
    }
}

