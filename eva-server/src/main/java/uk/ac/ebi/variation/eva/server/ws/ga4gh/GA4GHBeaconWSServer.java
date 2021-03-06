package uk.ac.ebi.variation.eva.server.ws.ga4gh;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
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
import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor;
import uk.ac.ebi.variation.eva.lib.datastore.DBAdaptorConnector;
import uk.ac.ebi.variation.eva.server.ws.EvaWSServer;

/**
 *
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
@Path("/{version}/ga4gh")
@Produces(MediaType.APPLICATION_JSON)
public class GA4GHBeaconWSServer extends EvaWSServer {
    
    public GA4GHBeaconWSServer() {
        super();
    }

    public GA4GHBeaconWSServer(@DefaultValue("") @PathParam("version")String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr) {
        super(version, uriInfo, hsr);
    }

    @GET
    @Path("/beacon")
    public Response beacon(@QueryParam("referenceName") String chromosome,
                           @QueryParam("start") Integer start,
                           @QueryParam("allele") String allele,
                           @QueryParam("datasetIds") String studies) 
            throws UnknownHostException, IllegalOpenCGACredentialsException, IOException {
        
        if (chromosome == null || chromosome.isEmpty() ||
                start == null || start < 0 || allele == null) {
            return createJsonResponse(new GA4GHBeaconResponse(chromosome, start, allele, studies, 
                    "Please provide chromosome, positive position and alternate allele"));
        }
        
        VariantDBAdaptor variantMongoDbAdaptor = DBAdaptorConnector.getVariantDBAdaptor("hsapiens_grch37");
        
        Region region = new Region(chromosome, start, start + allele.length());
        if (allele.equalsIgnoreCase("INDEL")) {
            queryOptions.put("type", "INDEL");
        } else {
            queryOptions.put("alternate", allele);
        }
        
        if (studies != null && !studies.isEmpty()) {
            queryOptions.put("studies", Arrays.asList(studies.split(",")));
        }
        
        QueryResult queryResult = variantMongoDbAdaptor.getAllVariantsByRegion(region, queryOptions);
//        queryResult.setResult(Arrays.asList(queryResult.getNumResults() > 0));
//        queryResult.setResultType(Boolean.class.getCanonicalName());
        return createJsonResponse(new GA4GHBeaconResponse(chromosome, start, allele, studies, queryResult.getNumResults() > 0));
    }
    
    class GA4GHBeaconResponse {
        
        private String chromosome;
        
        private Integer start;
        
        private String allele;
        
        private String datasetIds;
        
        private boolean exists;
        
        private String errorMessage;

        public GA4GHBeaconResponse(String chromosome, Integer start, String allele, String datasetIds, boolean exists) {
            this.chromosome = chromosome;
            this.start = start;
            this.allele = allele;
            this.datasetIds = datasetIds;
            this.exists = exists;
        }

        public GA4GHBeaconResponse(String chromosome, Integer start, String allele, String datasetIds, String errorMessage) {
            this.chromosome = chromosome;
            this.start = start;
            this.allele = allele;
            this.datasetIds = datasetIds;
            this.errorMessage = errorMessage;
        }

        public String getChromosome() {
            return chromosome;
        }

        public Integer getStart() {
            return start;
        }

        public String getAllele() {
            return allele;
        }

        public String getDatasetIds() {
            return datasetIds;
        }

        public boolean isExists() {
            return exists;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
}
