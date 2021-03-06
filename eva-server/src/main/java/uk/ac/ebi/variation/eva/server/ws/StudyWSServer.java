package uk.ac.ebi.variation.eva.server.ws;

import com.mongodb.BasicDBObject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.net.UnknownHostException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.storage.core.adaptors.StudyDBAdaptor;
import org.opencb.opencga.storage.core.variant.adaptors.VariantSourceDBAdaptor;
import org.opencb.opencga.storage.mongodb.variant.DBObjectToVariantSourceConverter;
import uk.ac.ebi.variation.eva.lib.datastore.DBAdaptorConnector;
import uk.ac.ebi.variation.eva.lib.storage.metadata.StudyDgvaDBAdaptor;
import uk.ac.ebi.variation.eva.lib.storage.metadata.StudyEvaproDBAdaptor;
import uk.ac.ebi.variation.eva.server.exception.SpeciesException;
import uk.ac.ebi.variation.eva.server.exception.VersionException;

/**
 *
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
@Path("/{version}/studies")
@Produces("application/json")
@Api(value = "Study", description = "Study RESTful Web Services API")
public class StudyWSServer extends EvaWSServer {

    private StudyDBAdaptor studyDgvaDbAdaptor;
    private StudyDBAdaptor studyEvaproDbAdaptor;


    public StudyWSServer(@DefaultValue("") @PathParam("version") String version,
                         @Context UriInfo uriInfo, @Context HttpServletRequest hsr) throws IOException, NamingException {
        super(version, uriInfo, hsr);
        studyDgvaDbAdaptor = new StudyDgvaDBAdaptor();
        studyEvaproDbAdaptor = new StudyEvaproDBAdaptor();
    }

    @GET
    @Path("/{study}/files")
    @ApiOperation(httpMethod = "GET", value = "Retrieves all the files from a study", response = QueryResponse.class)
    public Response getFilesByStudy(@PathParam("study") String study,
                                    @QueryParam("species") String species) 
            throws UnknownHostException, IllegalOpenCGACredentialsException, IOException {
        try {
            checkParams();
        } catch (VersionException | SpeciesException ex) {
            return createErrorResponse(ex.toString());
        }
            
        StudyDBAdaptor studyMongoDbAdaptor = DBAdaptorConnector.getStudyDBAdaptor(species);
        VariantSourceDBAdaptor variantSourceDbAdaptor = DBAdaptorConnector.getVariantSourceDBAdaptor(species);
        
        QueryResult idQueryResult = studyMongoDbAdaptor.findStudyNameOrStudyId(study, queryOptions);
        if (idQueryResult.getNumResults() == 0) {
            QueryResult queryResult = new QueryResult();
            queryResult.setErrorMsg("Study identifier not found");
            return createOkResponse(queryResult);
        }

        BasicDBObject id = (BasicDBObject) idQueryResult.getResult().get(0);
        QueryResult finalResult = variantSourceDbAdaptor.getAllSourcesByStudyId(
                id.getString(DBObjectToVariantSourceConverter.STUDYID_FIELD), queryOptions);
        finalResult.setDbTime(finalResult.getDbTime() + idQueryResult.getDbTime());

        return createOkResponse(finalResult);
    }

    @GET
    @Path("/{study}/view")
    @ApiOperation(httpMethod = "GET", value = "The info of a study", response = QueryResponse.class)
    public Response getStudy(@PathParam("study") String study,
                             @QueryParam("species") String species) 
            throws UnknownHostException, IllegalOpenCGACredentialsException, IOException {
        try {
            checkParams();
        } catch (VersionException | SpeciesException ex) {
            return createErrorResponse(ex.toString());
        }
        
        StudyDBAdaptor studyMongoDbAdaptor = DBAdaptorConnector.getStudyDBAdaptor(species);
        
        QueryResult idQueryResult = studyMongoDbAdaptor.findStudyNameOrStudyId(study, queryOptions);
        if (idQueryResult.getNumResults() == 0) {
            QueryResult queryResult = new QueryResult();
            queryResult.setErrorMsg("Study identifier not found");
            return createOkResponse(queryResult);
        }

        BasicDBObject id = (BasicDBObject) idQueryResult.getResult().get(0);
        QueryResult finalResult = studyMongoDbAdaptor.getStudyById(
            id.getString(DBObjectToVariantSourceConverter.STUDYID_FIELD), queryOptions);
        finalResult.setDbTime(finalResult.getDbTime() + idQueryResult.getDbTime());

        return createOkResponse(finalResult);
    }

    @GET
    @Path("/{study}/summary")
    public Response getStudySummary(@PathParam("study") String study,
                                    @DefaultValue("false") @QueryParam("structural") boolean structural) {
        if (structural) {
            return createOkResponse(studyDgvaDbAdaptor.getStudyById(study, queryOptions));
        } else {
            return createOkResponse(studyEvaproDbAdaptor.getStudyById(study, queryOptions));
        }
    }
}
