package uk.ac.ebi.variation.eva.server.ws;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.lib.auth.MongoCredentials;
import org.opencb.opencga.storage.variant.VariantDBAdaptor;
import org.opencb.opencga.storage.variant.mongodb.VariantMongoDBAdaptor;

/**
 *
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
@Path("/{version}/variants/{variantId}")
@Produces(MediaType.APPLICATION_JSON)
public class VariantWSServer extends EvaWSServer {

    private VariantDBAdaptor variantMongoQueryBuilder;
    private MongoCredentials credentials;

    public VariantWSServer() {

    }

    public VariantWSServer(@DefaultValue("") @PathParam("version")String version, @Context UriInfo uriInfo, @Context HttpServletRequest hsr) throws IOException {
        super(version, uriInfo, hsr);
        try {
            credentials = new MongoCredentials("mongos-hxvm-dev-001", 27017, "eva_hsapiens", "biouser", "biopass");
            variantMongoQueryBuilder = new VariantMongoDBAdaptor(credentials);
        } catch (IllegalOpenCGACredentialsException e) {
            e.printStackTrace();
        }
    }

    @GET
    @Path("/info")
    public Response getVariantById(@PathParam("variantId") String variantId) {
        if (!variantId.contains(":")) { // Query by accession id
            return createOkResponse(variantMongoQueryBuilder.getVariantById(variantId, queryOptions));
        } else { // Query by chr:pos:ref:alt
            String parts[] = variantId.split(":", -1);
            if (parts.length < 3) {
                return createJsonResponse("Invalid position and alleles combination, please use chr:pos:ref or chr:pos:ref:alt");
            }
            
            Region region = new Region(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[1]));
            queryOptions.put("reference", parts[2]);
            if (parts.length > 3) {
                queryOptions.put("alternate", parts[3]);
            }
            
            return createOkResponse(variantMongoQueryBuilder.getAllVariantsByRegion(region, queryOptions));
        }
    }
    
    @GET
    @Path("/exists")
    public Response checkVariantExists(@PathParam("variantId") String variantId) {
        if (!variantId.contains(":")) { // Query by accession id
            return createJsonResponse("Invalid position and alleles combination, please use chr:pos:ref or chr:pos:ref:alt");
        } else { // Query by chr:pos:ref:alt
            String parts[] = variantId.split(":", -1);
            if (parts.length < 3) {
                return createJsonResponse("Invalid position and alleles combination, please use chr:pos:ref or chr:pos:ref:alt");
            }
            
            Region region = new Region(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[1]));
            queryOptions.put("reference", parts[2]);
            if (parts.length > 3) {
                queryOptions.put("alternate", parts[3]);
            }
            
            QueryResult queryResult = variantMongoQueryBuilder.getAllVariantsByRegion(region, queryOptions);
            queryResult.setResult(Arrays.asList(queryResult.getNumResults() > 0));
            queryResult.setResultType(Boolean.class);
            return createOkResponse(queryResult);
        }
    }
   
}
