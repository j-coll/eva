package uk.ac.ebi.variation.eva.server.ws;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.storage.core.adaptors.ArchiveDBAdaptor;
import org.opencb.opencga.storage.core.adaptors.StudyDBAdaptor;
import uk.ac.ebi.variation.eva.lib.datastore.DBAdaptorConnector;
import uk.ac.ebi.variation.eva.lib.storage.metadata.ArchiveDgvaDBAdaptor;
import uk.ac.ebi.variation.eva.lib.storage.metadata.ArchiveEvaproDBAdaptor;
import uk.ac.ebi.variation.eva.lib.storage.metadata.StudyDgvaDBAdaptor;
import uk.ac.ebi.variation.eva.lib.storage.metadata.StudyEvaproDBAdaptor;

/**
 *
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
@Path("/{version}/meta")
@Produces(MediaType.APPLICATION_JSON)
public class ArchiveWSServer extends EvaWSServer {
    
    private ArchiveDBAdaptor archiveDgvaDbAdaptor;
    private ArchiveDBAdaptor archiveEvaproDbAdaptor;
    
    private StudyDBAdaptor studyDgvaDbAdaptor;
    private StudyDBAdaptor studyEvaproDbAdaptor;
    
    public ArchiveWSServer() {
        super();
    }

    public ArchiveWSServer(@DefaultValue("") @PathParam("version")String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr) 
            throws NamingException {
        super(version, uriInfo, hsr);
        archiveDgvaDbAdaptor = new ArchiveDgvaDBAdaptor();
        archiveEvaproDbAdaptor = new ArchiveEvaproDBAdaptor();
        studyDgvaDbAdaptor = new StudyDgvaDBAdaptor();
        studyEvaproDbAdaptor = new StudyEvaproDBAdaptor();
    }

    @GET
    @Path("/files/count")
    public Response countFiles() {
        return createOkResponse(archiveEvaproDbAdaptor.countFiles());
    }
    
    @GET
    @Path("/species/count")
    public Response countSpecies() {
        return createOkResponse(archiveEvaproDbAdaptor.countSpecies());
    }
    @GET
    @Path("/species/list")
    public Response getSpecies(@DefaultValue("false") @QueryParam("loaded") boolean loaded) {
        try {
            Properties properties = new Properties();
            properties.load(DBAdaptorConnector.class.getResourceAsStream("/mongo.properties"));
            
            return createOkResponse(archiveEvaproDbAdaptor.getSpecies(properties.getProperty("eva.version"), loaded));
        } catch (IOException ex) {
            return createErrorResponse(ex.toString());
        }
    }
    
    @GET
    @Path("/studies/count")
    public Response countStudies() {
        return createOkResponse(archiveEvaproDbAdaptor.countStudies());
    }
    
    @GET
    @Path("/studies/list")
    public Response getStudies(@QueryParam("species") String species) 
            throws UnknownHostException, IllegalOpenCGACredentialsException, IOException {
        StudyDBAdaptor studyMongoDbAdaptor = DBAdaptorConnector.getStudyDBAdaptor(species);
        return createOkResponse(studyMongoDbAdaptor.listStudies());
    }
    
    @GET
    @Path("/studies/all")
    public Response getStudies(@QueryParam("species") String species,
                               @QueryParam("type") String types,
                               @DefaultValue("false") @QueryParam("structural") boolean structural) {
        if (species != null && !species.isEmpty()) {
            queryOptions.put("species", Arrays.asList(species.split(",")));
        }
        if (types != null && !types.isEmpty()) {
            queryOptions.put("type", Arrays.asList(types.split(",")));
        }
        
        if (structural) {
            return createOkResponse(studyDgvaDbAdaptor.getAllStudies(queryOptions));
        } else {
            return createOkResponse(studyEvaproDbAdaptor.getAllStudies(queryOptions));
        }
    }
    
    @GET
    @Path("/studies/stats")
    public Response getStudiesStats(@QueryParam("species") String species,
                                    @DefaultValue("false") @QueryParam("structural") boolean structural) {
        if (species != null && !species.isEmpty()) {
            queryOptions.put("species", Arrays.asList(species.split(",")));
        }
        
        QueryResult<Map.Entry<String, Integer>> resultSpecies, resultTypes;
        
        if (structural) {
            resultSpecies = archiveDgvaDbAdaptor.countStudiesPerSpecies(queryOptions);
            resultTypes = archiveDgvaDbAdaptor.countStudiesPerType(queryOptions);
        } else {
            resultSpecies = archiveEvaproDbAdaptor.countStudiesPerSpecies(queryOptions);
            resultTypes = archiveEvaproDbAdaptor.countStudiesPerType(queryOptions);
        }
        
        QueryResult combinedQueryResult = new QueryResult();
        combinedQueryResult.setDbTime(resultSpecies.getDbTime() + resultTypes.getDbTime());
        
        JsonNodeFactory factory = new JsonNodeFactory(true);
        ObjectNode root = factory.objectNode();
        combinedQueryResult.addResult(root);
        combinedQueryResult.setNumTotalResults(combinedQueryResult.getNumResults());
        
        // Species
        ObjectNode speciesNode = factory.objectNode();
        for (Map.Entry<String, Integer> speciesCount : resultSpecies.getResult()) {
            speciesNode.put(speciesCount.getKey(), speciesCount.getValue());
        }
        root.put("species", speciesNode);
        
        // Types
        ObjectNode typesNode = factory.objectNode();
        for (Map.Entry<String, Integer> typesCount : resultTypes.getResult()) {
            typesNode.put(typesCount.getKey(), typesCount.getValue());
        }
        root.put("type", typesNode);
        
        return createOkResponse(combinedQueryResult);
    }
}
