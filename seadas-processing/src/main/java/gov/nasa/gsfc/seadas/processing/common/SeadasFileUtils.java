package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.core.runtime.RuntimeContext;
import gov.nasa.gsfc.seadas.processing.core.*;
import gov.nasa.gsfc.seadas.ocssw.OCSSWClient;
import gov.nasa.gsfc.seadas.ocssw.OCSSWOldModel;
import org.apache.commons.lang.ArrayUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 6/20/12
 * Time: 2:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class SeadasFileUtils {

    private static boolean debug = false;
    private static String NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME = "next_level_name.py";
    private static String NEXT_LEVEL_FILE_NAME_TOKEN = "Output Name:";

    public static File createFile(String parent, String fileName) {
        File pFile;
        if (parent == null) {
            pFile = null;
        } else {
            pFile = new File(parent);
        }
        return createFile(pFile, fileName);
    }

    public static File createFile(File parent, String fileName) {
        if (fileName == null) {
            return null;
        }

        String expandedFilename = SeadasFileUtils.expandEnvironment(fileName);
        File file = new File(expandedFilename);
        if (!file.isAbsolute() && parent != null) {
            file = new File(parent, expandedFilename);
        }
        return file;
    }

    public static String getCurrentDate(String dateFormat) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(cal.getTime());

    }

    public static String getKeyValueFromParFile(File file, String key) {
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(file));
            String line;
            line = reader.readLine();
            String[] option;
            ParamInfo pi;
            while (line != null) {
                if (line.indexOf("=") != -1) {
                    option = line.split("=", 2);
                    option[0] = option[0].trim();
                    option[1] = option[1].trim();
                    if (option[0].trim().equals(key)) {
                        return option[1];
                    }
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException fnfe) {

        } catch (IOException ioe) {

        }
        return null;
    }

    public static String getGeoFileNameFromIFile(String ifileName) {

        String geoFileName = (ifileName.substring(0, ifileName.indexOf("."))).concat(".GEO");
        int pos1 = ifileName.indexOf(".");
        int pos2 = ifileName.lastIndexOf(".");

        if (pos2 > pos1) {
            geoFileName = ifileName.substring(0, pos1 + 1) + "GEO" + ifileName.substring(pos2);
        }

        if (new File(geoFileName).exists()) {
            return geoFileName;
        } else {
            VisatApp.getApp().showErrorDialog(ifileName + " requires a GEO file to be extracted. " + geoFileName + " does not exist.");
            return null;
        }
    }

    private static String retrieveOFileNameRemote(String[] cmdArray) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (String s : cmdArray) {
            jab.add(s);
        }

        JsonArray remoteCmdArray = jab.build();

        OCSSWClient ocsswClient = new OCSSWClient();
        WebTarget target = ocsswClient.getOcsswWebTarget();
        final Response response = target.path("ocssw").path("computeNextLevelFileName").path(OCSSWOldModel.getJobId()).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(remoteCmdArray, MediaType.APPLICATION_JSON_TYPE));

        String ofileName = target.path("ocssw").path("retrieveNextLevelFileName").path(OCSSWOldModel.getJobId()).request(MediaType.TEXT_PLAIN).get(String.class);
        if (ofileName != null) {
            return ofileName;
        } else {
            return "output";
        }
    }

    private static String[] getCmdArrayForNextLevelNameFinder(String ifileName, String programName) {
        String[] cmdArray = new String[6];
        cmdArray[0] = OCSSWOldModel.getOcsswRunnerScriptPath();
        cmdArray[1] = "--ocsswroot";
        cmdArray[2] = OCSSWOldModel.getOcsswEnv();
        cmdArray[3] = NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME;
        cmdArray[4] = RuntimeContext.getConfig().getContextProperty(OCSSWOldModel.OCSSW_LOCATION_PROPERTY).equals(OCSSWOldModel.SEADAS_OCSSW_LOCATION_LOCAL) ? ifileName : getIfileNameforRemoteServer(ifileName);

        cmdArray[5] = programName;
        return cmdArray;
    }

    private String getIfileNameforCmdArray(String ifileName){
        if (RuntimeContext.getConfig().getContextProperty(OCSSWOldModel.OCSSW_LOCATION_PROPERTY).equals(OCSSWOldModel.SEADAS_OCSSW_LOCATION_LOCAL)){
            return ifileName;
        } else {
            return ifileName;
        }
    }

    private static String getIfileNameforRemoteServer(String ifileName) {
        String newIfileName = ifileName.replace(OCSSWOldModel.getOCSSWClientSharedDirName(), OCSSWOldModel.getServerSharedDirName());
        return newIfileName;
    }

    public static String expandEnvironment(String string1) {

        if (string1 == null) {
            return string1;
        }

        String environmentPattern = "([A-Za-z0-9_]+)";
        Pattern pattern1 = Pattern.compile("\\$\\{" + environmentPattern + "\\}");
        Pattern pattern2 = Pattern.compile("\\$" + environmentPattern);


        String string2 = null;

        while (!string1.equals(string2)) {
            if (string2 != null) {
                string1 = string2;
            }

            string2 = expandEnvironment(pattern1, string1);
            string2 = expandEnvironment(pattern2, string2);

            if (string2 == null) {
                return string2;
            }
        }

        return string2;
    }


    private static String expandEnvironment(Pattern pattern, String string) {


        if (string == null || pattern == null) {
            return string;
        }

        Matcher matcher = pattern.matcher(string);
        Map<String, String> envMap = null;

        while (matcher.find()) {

            // Retrieve environment variables
            if (envMap == null) {
                envMap = System.getenv();
            }

            String envNameOnly = matcher.group(1);

            if (envMap.containsKey(envNameOnly)) {
                String envValue = envMap.get(envNameOnly);

                if (envValue != null) {
                    String escapeStrings[] = {"\\", "$", "{", "}"};
                    for (String escapeString : escapeStrings) {
                        envValue = envValue.replace(escapeString, "\\" + escapeString);
                    }

                    Pattern envNameClausePattern = Pattern.compile(Pattern.quote(matcher.group(0)));
                    string = envNameClausePattern.matcher(string).replaceAll(envValue);
                }
            }
        }

        return string;
    }


    public static void debug(String message) {
        if (debug) {
            System.out.println("Debugging: " + message);
        }
    }

    public static void writeToDisk(String fileName, String log) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(new File(fileName));
            fileWriter.write(log);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    public String tail( File file ) {
        RandomAccessFile fileHandler = null;
        try {
            fileHandler = new RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();

            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                if( readByte == 0xA ) {
                    if( filePointer == fileLength ) {
                        continue;
                    }
                    break;

                } else if( readByte == 0xD ) {
                    if( filePointer == fileLength - 1 ) {
                        continue;
                    }
                    break;
                }

                sb.append( ( char ) readByte );
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch( java.io.FileNotFoundException e ) {
            e.printStackTrace();
            return null;
        } catch( java.io.IOException e ) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                /* ignore */
                }
        }
    }

    public String tail2( File file, int lines) {
        java.io.RandomAccessFile fileHandler = null;
        try {
            fileHandler =
                    new java.io.RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                if( readByte == 0xA ) {
                    if (filePointer < fileLength) {
                        line = line + 1;
                    }
                } else if( readByte == 0xD ) {
                    if (filePointer < fileLength-1) {
                        line = line + 1;
                    }
                }
                if (line >= lines) {
                    break;
                }
                sb.append( ( char ) readByte );
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch( java.io.FileNotFoundException e ) {
            e.printStackTrace();
            return null;
        } catch( java.io.IOException e ) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                }
        }
    }
 }