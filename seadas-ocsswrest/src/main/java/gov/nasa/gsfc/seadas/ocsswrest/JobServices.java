package gov.nasa.gsfc.seadas.ocsswrest;

import gov.nasa.gsfc.seadas.ocsswrest.database.SQLiteJDBC;
import gov.nasa.gsfc.seadas.ocsswrest.utilities.OCSSWServerModelOld;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 2/5/15
 * Time: 4:20 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/jobs")
public class JobServices {
    @GET
    @Path("/newJobId") ///{clientId}/{processorId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String createNewJob() {
        String newJobId = hashJobID(new Long(new Date().getTime()).toString());
        //insert rows for this new job in the processor and file tables.
        OCSSWServerModelOld.setCurrentJobId(newJobId);
        System.out.println(" in job services: " + newJobId);
        SQLiteJDBC.insertItem("PROCESSOR_TABLE", "JOB_ID", newJobId);
        return newJobId;
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getJobList() {
        JsonObject jobList = Json.createObjectBuilder().build();
        return jobList;
    }

    @DELETE
    @Path("/deleteJobs")
    public void deleteAllJobs() {

    }

    @DELETE
    @Path("/deleteJobId")
    @Consumes(MediaType.TEXT_PLAIN)
    public void deleteAJob(String jobId) {

    }

    private String hashJobID(String jobID) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        md.update(jobID.getBytes());

        byte byteData[] = md.digest();
        //convert the byte to hex format
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

//    @Path("/{clientId}/{processorId}/{jobId}")
//    public void updateFiles(){
//
//    }
}
