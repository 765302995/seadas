package gov.nasa.gsfc.seadas.ocsswrest;

import gov.nasa.gsfc.seadas.ocsswrest.utilities.OCSSWServerPropertyValues;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 12/29/14
 * Time: 5:23 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/file")
public class OCSSWFileServices {

    private static final String OCSSW_PROCESSING_DIR = "ocsswfiles";
    private static final String FILE_UPLOAD_PATH = System.getProperty("user.home") + System.getProperty("file.separator") + OCSSW_PROCESSING_DIR + System.getProperty("file.separator") + "ifiles";
    private static final String FILE_DOWNLOAD_PATH = System.getProperty("user.home") + System.getProperty("file.separator") + OCSSW_PROCESSING_DIR + System.getProperty("file.separator") + "ofiles";
    private static final String FILE_DIR = System.getProperty("user.dir") + System.getProperty("file.separator") + "files";
    private static final String OCSSW_OUTPUT_COMPRESSED_FILE_NAME = "ocssw_output.zip";
    private static final int BUFFER_SIZE = 1024;

    @GET
    @Path("/serverSharedFileDir")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSharedFileDirName() {
        System.out.println("Shared dir name:" + OCSSWServerPropertyValues.getServerSharedDirName());
        return OCSSWServerPropertyValues.getServerSharedDirName();
    }

    /**
     * Method for uploading a file.
     * handling HTTP POST requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response fileUpload(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileInfo,
            @FormDataParam("clientId") String clientID,
            @FormDataParam("processorId") String processorId,
            @FormDataParam("jobId") String jobId)
            throws IOException {
        Response.Status respStatus = Response.Status.OK;
        if (fileInfo == null) {
            respStatus = Response.Status.INTERNAL_SERVER_ERROR;
        } else {
            final String fileName = fileInfo.getFileName();
            String uploadedFileDir = FILE_UPLOAD_PATH + File.separator + clientID + File.separator + processorId + File.separator + jobId;
            new File(uploadedFileDir).mkdirs();
            String uploadedFileLocation = uploadedFileDir + File.separator + fileName;
            System.out.println(uploadedFileLocation);
            System.out.println(System.getProperty("user.home"));
            System.out.println(new File(uploadedFileDir).getAbsolutePath());
            try {
                writeToFile(uploadedInputStream, uploadedFileLocation);
                //getFileInfo();
            } catch (Exception e) {
                respStatus = Response.Status.INTERNAL_SERVER_ERROR;
                e.printStackTrace();
            }
        }
        return Response.status(respStatus).build();
    }

    @POST
    @Path("/upload/{clientId}/{processorId}/{jobId}/{fileName}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response largeFileUpload(
            @PathParam("clientId") String clientId,
            @PathParam("processorId") String processorId,
            @PathParam("jobId") String jobId,
            @PathParam("fileName") String fileName,
            InputStream uploadedInputStream)
            throws IOException {
        Response.Status respStatus = Response.Status.OK;
        if (fileName == null) {
            respStatus = Response.Status.INTERNAL_SERVER_ERROR;
        } else {
            //final String fileName = fileInfo.getFileName();
            String uploadedFileDir = FILE_UPLOAD_PATH + File.separator + clientId + File.separator + processorId + File.separator + jobId;
            File newFile = new File(uploadedFileDir);
            Files.createDirectories(newFile.toPath());
            //mkDirs(clientId, processorId, jobId);

            boolean isDirCreated = new File(uploadedFileDir).isDirectory();
            String uploadedFileLocation = uploadedFileDir + File.separator + fileName;

            System.out.println(uploadedFileLocation + " is created " + isDirCreated);
            System.out.println(System.getProperty("user.home"));
            System.out.println(new File(uploadedFileDir).getAbsolutePath());
            try {
                writeToFile(uploadedInputStream, uploadedFileLocation);
                //getFileInfo();
            } catch (Exception e) {
                respStatus = Response.Status.INTERNAL_SERVER_ERROR;
                e.printStackTrace();
            }
        }
        return Response.status(respStatus).build();
    }

    private void mkDirs(String clientId, String processorId, String jobId) {
        File dir = new File(FILE_UPLOAD_PATH);
        dir.mkdirs();
        dir = new File(dir, clientId);
        dir.mkdirs();
        dir = new File(dir, processorId);
        dir.mkdirs();
        dir = new File(dir, jobId);
        dir.mkdirs();
    }

    // save uploaded file to new location
    private void writeToFile(InputStream uploadedInputStream,
                             String uploadedFileLocation) {

        try {
            File file = new File(uploadedFileLocation);
            //Files.copy(uploadedInputStream, file.toPath());

            OutputStream out = new FileOutputStream(file);
            //Files.copy(file.toPath(), out);
            int read = 0;
            byte[] bytes = new byte[8192];

            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            uploadedInputStream.close();
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    @GET
    @Path("/download")
    //@Produces("application/octet-stream")
    public File downloadFile(@PathParam("fileName") String fileName) {
        File file = new File(FILE_UPLOAD_PATH + "/A2002365234500.L2_LAC");

        //Put some validations here such as invalid file name or missing file name
        if (!file.exists()) {
            Response.ResponseBuilder response = Response.status(Response.Status.BAD_REQUEST);
            return null;
        }
        System.out.println(file.getAbsolutePath());
        return file;
        //Response.ResponseBuilder responseBuilder = Response.ok((Object) file);
        //responseBuilder.header("Content-Disposition", "attachment; filename=`howtodoinjava.txt'");
        //return responseBuilder.build();
    }

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        //return "Got it! \n";
        System.out.println("getting ocssw shared server name");
        OCSSWServerPropertyValues propertyValues = new OCSSWServerPropertyValues();
        return propertyValues.getServerSharedDirName();
    }


    @GET
    @Path("/missionInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getFileMissionInfo() {
        JsonObject jsonObject = Json.createObjectBuilder().build();
        return jsonObject;
    }

}

