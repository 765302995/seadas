package gov.nasa.gsfc.seadas.ocssw;


import com.bc.ceres.core.runtime.RuntimeContext;
import org.esa.beam.visat.VisatApp;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 12/9/14
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class OCSSWClient {

    //public static final String RESOURCE_BASE_URI = RuntimeContext.getConfig().getContextProperty(OCSSW.OCSSW_LOCATION_PROPERTY);//"http://localhost:6400/ocsswws/";
    public static final String defaultServer ="localhost";
    public static final String defaultPort = "6400";
    final static String OCSSW_REST_SERVICES_CONTEXT_PATH = "ocsswws";

    private WebTarget target;
    private String resourceBaseUri;

    public OCSSWClient(String serverIPAddress, String portNumber) {
        resourceBaseUri = getResourceBaseUri(serverIPAddress, portNumber);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);
        clientConfig.register(JsonProcessingFeature.class).property(JsonGenerator.PRETTY_PRINTING, true);
        Client c = ClientBuilder.newClient(clientConfig);
        target = c.target(resourceBaseUri);
    }

    public OCSSWClient(){
      this(defaultServer, defaultPort);
    }

    public WebTarget getOcsswWebTarget() {
        return target;
    }

    private String getResourceBaseUri(String serverIPAddress, String portNumber){
        String resourceBaseUri = "http://" + serverIPAddress + ":" + portNumber + "/" + OCSSW_REST_SERVICES_CONTEXT_PATH + "/";
        return  resourceBaseUri;
    }

}

