package gov.nasa.gsfc.seadas.ocssw;

import com.bc.ceres.core.runtime.RuntimeContext;
import com.sun.deploy.security.ValidationState;
import gov.nasa.gsfc.seadas.processing.common.ParFileManager;
import gov.nasa.gsfc.seadas.processing.common.SeadasLogger;
import gov.nasa.gsfc.seadas.processing.common.SeadasProcess;
import gov.nasa.gsfc.seadas.processing.core.ParamInfo;
import gov.nasa.gsfc.seadas.processing.core.ParamList;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.utilities.SeadasArrayUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import javax.activation.MimeType;
import javax.json.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by aabduraz on 3/27/17.
 */
public class OCSSWRemoteClient extends OCSSW {

    public static final String OCSSW_SERVER_PORT_NUMBER = "6401";
    public static final String SEADAS_CLIENT_ID_PROPERTY = "client.id";
    public static final String MLP_PROGRAM_NAME = "multilevel_processor.py";

    WebTarget target;
    String jobId;
    boolean ifileUploadSuccess;
    String ofileName;


    public OCSSWRemoteClient() {
        initiliaze();
    }

    private void initiliaze() {
        String remoteServerIPAddress = RuntimeContext.getConfig().getContextProperty(OCSSW_LOCATION_PROPERTY, "localhost");
        String remoteServerPortNumber = OCSSW_SERVER_PORT_NUMBER;
        OCSSWClient ocsswClient = new OCSSWClient(remoteServerIPAddress, remoteServerPortNumber);
        target = ocsswClient.getOcsswWebTarget();
        JsonObject jsonObject = target.path("ocssw").path("ocsswInfo").request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
        ocsswExist = jsonObject.getBoolean("ocsswExists");
        ocsswRoot = jsonObject.getString("ocsswRoot");
        if (ocsswExist) {
            jobId = target.path("jobs").path("newJobId").request(MediaType.TEXT_PLAIN_TYPE).get(String.class);
            String clientId = RuntimeContext.getConfig().getContextProperty(SEADAS_CLIENT_ID_PROPERTY, System.getProperty("user.home"));
            target.path("ocssw").path("ocsswSetClientId").path(jobId).request().put(Entity.entity(clientId, MediaType.TEXT_PLAIN_TYPE));
            ocsswDataDirPath = jsonObject.getString("ocsswDataDirPath");
            ocsswInstallerScriptPath = jsonObject.getString("ocsswInstallerScriptPath");
            ocsswRunnerScriptPath = jsonObject.getString("ocsswRunnerScriptPath");
            ocsswScriptsDirPath = jsonObject.getString("ocsswScriptsDirPath");
        }
    }

    @Override
    public void setProgramName(String programName) {

        this.programName = programName;
        Response response = target.path("ocssw").path("ocsswSetProgramName").path(jobId).request().put(Entity.entity(programName, MediaType.TEXT_PLAIN_TYPE));
    }


    @Override
    public void setIfileName(String ifileName) {
        this.ifileName = ifileName;
        if (uploadIFile(ifileName)) {
            ifileUploadSuccess = true;
        } else {
            ifileUploadSuccess = false;
        }

    }

    private void updateProgramName(String programName) {
        this.programName = programName;
        setXmlFileName(programName + ".xml");
    }


    public boolean uploadIFile(String ifileName) {
        final FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", new File(ifileName));
        final MultiPart multiPart = new FormDataMultiPart()
                //.field("ifileName", ifileName)
                .bodyPart(fileDataBodyPart);
        Response response = target.path("fileServices").path("uploadFile").path(jobId).request().post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));
        if (response.getStatus() == Response.ok().build().getStatus()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean uploadClientFile(String fileName) {
        final FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", new File(fileName));
        final MultiPart multiPart = new FormDataMultiPart()
                //.field("ifileName", ifileName)
                .bodyPart(fileDataBodyPart);
        Response response = target.path("fileServices").path("uploadClientFile").path(jobId).request().post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));
        uploadListedFiles(fileName);
        if (response.getStatus() == Response.ok().build().getStatus()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method uploads list of files provided in the text file.
     * @param fileName is the name of the text file that contains list of input files.
     * @return true if all files uploaded successfully.
     */
    public boolean uploadListedFiles(String fileName) {

        File file = new File(fileName);
        Scanner input = null;
        try {
            input = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        FileDataBodyPart fileDataBodyPart;
        MultiPart multiPart;
        Response response;
        String nextFileName;
        boolean fileUploadSuccess = true;

        while (input.hasNext()) {
            nextFileName = input.nextLine();
            fileDataBodyPart = new FileDataBodyPart("inputFile", new File(nextFileName));
            multiPart = new FormDataMultiPart()
                    //.field("ifileName", ifileName)
                    .bodyPart(fileDataBodyPart);
            response = target.path("fileServices").path("uploadClientFile").path(jobId).request().post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));
            if (response.getStatus() == Response.ok().build().getStatus()) {
                fileUploadSuccess = fileUploadSuccess & true;
            } else {
                return fileUploadSuccess = fileUploadSuccess & false;
            }
        }

        input.close();

        return fileUploadSuccess;

    }


    @Override
    public String getOfileName(String ifileName) {

        if (programName.equals("l3bindump")) {
            return ifileName + ".xml";
        }
        this.setIfileName(ifileName);

        if (ifileUploadSuccess) {
            JsonObject jsonObject = target.path("ocssw").path("getOfileName").path(jobId).request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
            ofileName = jsonObject.getString("ofileName");
            missionName = jsonObject.getString("missionName");
            fileType = jsonObject.getString("fileType");
            updateProgramName(jsonObject.getString("programName"));
            ofileName = ifileName.substring(0, ifileName.lastIndexOf(File.separator) + 1) + ofileName;
            return ofileName;
        } else {
            return null;
        }

    }

    @Override
    public String getOfileName(String ifileName, String[] options) {

        if (programName.equals("l3bindump")) {
            return ifileName + ".xml";
        }
        this.setIfileName(ifileName);

        if (ifileUploadSuccess) {
            JsonObject jsonObjectForUpload = getNextLevelNameJsonObject(ifileName, options);
            target.path("ocssw").path("uploadNextLevelNameParams").path(jobId).request().post(Entity.entity(jsonObjectForUpload, MediaType.APPLICATION_JSON_TYPE));
            JsonObject jsonObject = target.path("ocssw").path("getOfileName").path(jobId).request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
            ofileName = jsonObject.getString("ofileName");
            missionName = jsonObject.getString("missionName");
            fileType = jsonObject.getString("fileType");
            updateProgramName(jsonObject.getString("programName"));
            ofileName = ifileName.substring(0, ifileName.lastIndexOf(File.separator) + 1) + ofileName;
            return ofileName;
        } else {
            return null;
        }
    }

    @Override
    public String getOfileName(String ifileName, String programName, String suiteValue) {

        this.programName = programName;
        String[] additionalOptions = {"--suite=" + suiteValue};
        return getOfileName(ifileName, additionalOptions);
    }

    private JsonObject getNextLevelNameJsonObject(String ifileName, String[] additionalOptions) {
        String additionalOptionsString = "";
        for (String s : additionalOptions) {
            additionalOptionsString = additionalOptionsString + s + " ; ";
        }
        JsonObject jsonObject = Json.createObjectBuilder().add("ifileName", ifileName)
                .add("programName", programName)
                .add("additionalOptions", additionalOptionsString)
                .build();
        return jsonObject;
    }

    private JsonArray transformToJsonArray(String[] commandArray) {

        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        for (String option : commandArray) {
            jsonArrayBuilder.add(option);
        }

        return jsonArrayBuilder.build();
    }


    private String getOfileName(JsonArray jsonArray) {
        JsonObject jsonObject = target.path("ocssw").path("getOfileName").path(OCSSWOldModel.getJobId()).request(MediaType.APPLICATION_JSON).put(Entity.entity(jsonArray, MediaType.APPLICATION_JSON), JsonObject.class);
        String ofileName = jsonObject.getString("ofileName");
        missionName = jsonObject.getString("missionName");
        fileType = jsonObject.getString("fileType");
        return ofileName;
    }

    @Override
    public boolean isMissionDirExist(String missionName) {
        Boolean isMissionExist = target.path("ocssw").path("isMissionDirExist").path(missionName).request().get(Boolean.class);
        return isMissionExist.booleanValue();
    }

    @Override
    public String[] getMissionSuites(String missionName, String programName) {
        return target.path("ocssw").path("missionSuites").path(missionName).path(programName).request(MediaType.APPLICATION_JSON_TYPE).get(String[].class);
    }

//    @Override
//    public Process execute(ProcessorModel processorModel) {
//        SeadasProcess seadasProcess = new SeadasProcess();
//        String programName = processorModel.getProgramName();
//        JsonObject commandArrayParamJsonObject = getJsonFromParamList(processorModel.getParamList());
//        Response response = target.path("ocssw").path("executeOcsswProgramOnDemand").path(jobId).path(programName).request().put(Entity.entity(commandArrayParamJsonObject, MediaType.APPLICATION_JSON_TYPE));
//        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
//            downloadFiles(commandArrayParamJsonObject);
//            seadasProcess.setExitValue(0);
//        }
//        return  seadasProcess;
//    }

    /**
     * this method returns a command array for execution.
     * the array is constructed using the paramList data and input/output files.
     * the command array structure is: full pathname of the program to be executed, input file name, params in the required order and finally the output file name.
     * assumption: order starts with 1
     *
     * @return
     */
    @Override
    public Process execute(ProcessorModel processorModel) {
        SeadasProcess seadasProcess = new SeadasProcess();

        JsonObject commandArrayJsonObject = null;

        String programName = processorModel.getProgramName();

        if (processorModel.acceptsParFile() && programName.equals(MLP_PROGRAM_NAME)) {
            String parString = processorModel.getParamList().getParamString("\n");
            ;
            File parFile = writeParFile(convertParStringForRemoteServer(parString));
            target.path("ocssw").path("uploadMLPParFile").path(jobId).request().put(Entity.entity(parFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            commandArrayJsonObject = Json.createObjectBuilder().add("parFile", parFile.getName()).build();
            target.path("ocssw").path("executeMLP").path(jobId).path(programName).request().get(String.class);

        } else {
            commandArrayJsonObject = getJsonFromParamList(processorModel.getParamList());
            Response response = target.path("ocssw").path("executeOcsswProgramOnDemand").path(jobId).path(programName).request().put(Entity.entity(commandArrayJsonObject, MediaType.APPLICATION_JSON_TYPE));
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                downloadFiles(commandArrayJsonObject);
                seadasProcess.setExitValue(0);
            }
        }


        return seadasProcess;
    }


    private void downloadFiles(JsonObject paramJsonObject) {
        Set commandArrayKeys = paramJsonObject.keySet();
        String param;
        String ofileFullPathName, ofileName;
        try {
            Object[] array = (Object[]) commandArrayKeys.toArray();
            int i = 0;
            String[] commandArray = new String[commandArrayKeys.size() + 1];
            commandArray[i++] = programName;
            for (Object element : array) {
                String elementName = (String) element;
                param = paramJsonObject.getString((String) element);
                if (elementName.contains("OFILE")) {
                    if (param.indexOf("=") != -1) {
                        StringTokenizer st = new StringTokenizer(param, "=");
                        String paramName = st.nextToken();
                        String paramValue = st.nextToken();
                        ofileFullPathName = paramValue;

                    } else {
                        ofileFullPathName = param;
                    }
                    ofileName = ofileFullPathName.substring(ofileFullPathName.lastIndexOf(File.separator) + 1);
                    Response response = target.path("fileServices").path("downloadFile").path(jobId).path(ofileName).request().get(Response.class);
                    InputStream responceStream = (InputStream) response.getEntity();
                    writeToFile(responceStream, ofileFullPathName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Process execute(ParamList paramListl) {
        JsonObject commandArrayJsonObject = getJsonFromParamList(paramListl);
        Response response = target.path("ocssw").path("executeOcsswProgram").path(jobId).request().put(Entity.entity(commandArrayJsonObject, MediaType.APPLICATION_JSON_TYPE));
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            Response output = target.path("fileServices").path("downloadFile").path(jobId).request().get(Response.class);
            final InputStream responseStream = (InputStream) output.getEntity();
            writeToFile(responseStream, ofileName);
        }
        Process seadasProcess = new SeadasProcess();
        return seadasProcess;
    }

    // save uploaded file to new location
    private void writeToFile(InputStream downloadedInputStream,
                             String downloadedFileLocation) {

        try {
            File file = new File(downloadedFileLocation);
            OutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            byte[] bytes = new byte[8192];

            while ((read = downloadedInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            downloadedInputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private JsonObject getJsonFromParamList(ParamList paramList) {

        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();


        Iterator itr = paramList.getParamArray().iterator();

        ParamInfo option;
        String commandItem;
        while (itr.hasNext()) {
            option = (ParamInfo) itr.next();
            commandItem = null;
            if (option.getType() != ParamInfo.Type.HELP) {
                if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_ARGUMENT)) {
                    if (option.getValue() != null && option.getValue().length() > 0) {
                        commandItem = option.getValue();
                    }
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_OPTION) && !option.getDefaultValue().equals(option.getValue())) {
                    commandItem = option.getName() + "=" + option.getValue();
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_FLAG) && (option.getValue().equals("true") || option.getValue().equals("1"))) {
                    if (option.getName() != null && option.getName().length() > 0) {
                        commandItem = option.getName();
                    }
                }
            }
            //need to send both item name and its type to accurately construct the command array on the server
            if (commandItem != null) {
                jsonObjectBuilder.add(option.getName() + "_" + option.getType(), commandItem);
            }

        }
        return jsonObjectBuilder.build();
    }

    private JsonArray getJsonFromParamListOld(ParamList paramList) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        Iterator itr = paramList.getParamArray().iterator();

        ParamInfo option;
        String optionValue;
        while (itr.hasNext()) {
            option = (ParamInfo) itr.next();
            optionValue = option.getValue();
            if (option.getType() != ParamInfo.Type.HELP) {
                if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_ARGUMENT)) {
                    if (option.getValue() != null && option.getValue().length() > 0) {
                        jsonArrayBuilder.add(optionValue);
                    }
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_OPTION) && !option.getDefaultValue().equals(option.getValue())) {
                    jsonArrayBuilder.add(option.getName() + "=" + optionValue);
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_FLAG) && (option.getValue().equals("true") || option.getValue().equals("1"))) {
                    if (option.getName() != null && option.getName().length() > 0) {
                        jsonArrayBuilder.add(option.getName());
                    }
                }
            }
        }
        JsonArray jsonCommandArray = jsonArrayBuilder.build();
        return jsonCommandArray;
    }

    private String convertParStringForRemoteServer(String parString) {
        StringTokenizer st1 = new StringTokenizer(parString, "\n");
        StringTokenizer st2;
        StringBuilder stringBuilder = new StringBuilder();
        String token;
        String key, value;
        String fileTypeString;
        while (st1.hasMoreTokens()) {
            token = st1.nextToken();
            if (token.contains("=")) {
                st2 = new StringTokenizer(token, "=");
                key = st2.nextToken();
                value = st2.nextToken();
                if (new File(value).exists()) {
                    uploadClientFile(value);
                    try {
                        fileTypeString = Files.probeContentType(new File(value).toPath());
                        if (fileTypeString.equals(MediaType.TEXT_PLAIN)) {
                            uploadListedFiles(value);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    value = value.substring(value.lastIndexOf(File.separator) + 1);
                }
                token = key + "=" + value;
            }
            stringBuilder.append(token);
            stringBuilder.append("\n");
        }

        String newParString = stringBuilder.toString();
        return newParString;
    }


    private File writeParFile(String parString) {

        try {

            final File tempFile = File.createTempFile(programName + "-tmpParFile", ".par");
            String parFileLocation = tempFile.getAbsolutePath();
            System.out.println(tempFile.getAbsoluteFile());
            tempFile.deleteOnExit();
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(tempFile);
                fileWriter.write(parString + "\n");
            } finally {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
            return tempFile;

        } catch (IOException e) {
            SeadasLogger.getLogger().warning("parfile is not created. " + e.getMessage());
            return null;
        }
    }

    @Override
    public Process execute(String[] commandArray) {
        return null;
    }

    @Override
    public Process execute(String programName, String[] commandArrayParams) {
        return null;
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

    @Override
    public void setCommandArrayPrefix() {

    }

    @Override
    public void setCommandArraySuffix() {

    }

    @Override
    public void setMissionName(String missionName) {

    }

    @Override
    public void setFileType(String fileType) {

    }
}

