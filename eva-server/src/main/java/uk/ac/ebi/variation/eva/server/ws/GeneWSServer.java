package uk.ac.ebi.variation.eva.server.ws;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor;
import uk.ac.ebi.variation.eva.lib.datastore.DBAdaptorConnector;
import uk.ac.ebi.variation.eva.server.exception.SpeciesException;
import uk.ac.ebi.variation.eva.server.exception.VersionException;

/**
 *
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
@Path("/{version}/genes")
@Produces("application/json")
@Api(value = "Gene", description = "Gene RESTful Web Services API")
public class GeneWSServer extends EvaWSServer {


    public GeneWSServer(@DefaultValue("") @PathParam("version")String version,
                        @Context UriInfo uriInfo, @Context HttpServletRequest hsr) {
        super(version, uriInfo, hsr);
    }

    @GET
    @Path("/{gene}/variants")
    @ApiOperation(httpMethod = "GET", value = "Retrieves all the variants of a gene", response = QueryResponse.class)
    public Response getVariantsByGene(@PathParam("gene") String geneId,
                                      @QueryParam("ref") String reference,
                                      @QueryParam("alt") String alternate,
                                      @QueryParam("studies") String studies,
                                      @QueryParam("species") String species,
                                      @DefaultValue("") @QueryParam("maf") String maf,
                                      @DefaultValue("-1") @QueryParam("miss_alleles") int missingAlleles,
                                      @DefaultValue("-1") @QueryParam("miss_gts") int missingGenotypes,
                                      @DefaultValue("") @QueryParam("type") String variantType)
            throws IllegalOpenCGACredentialsException, IOException {
        try {
            checkParams();
        } catch (VersionException | SpeciesException ex) {
            return createErrorResponse(ex.toString());
        }
        
        VariantDBAdaptor variantMongoDbAdaptor = DBAdaptorConnector.getVariantDBAdaptor(species);

        for (String acceptedValue : VariantDBAdaptor.QueryParams.acceptedValues) {
            if (uriInfo.getQueryParameters().containsKey(acceptedValue)) {
                queryOptions.add(acceptedValue, uriInfo.getQueryParameters().get(acceptedValue).get(0));
            }
        }

        if (reference != null) {
            queryOptions.put(VariantDBAdaptor.REFERENCE, reference);
        }
        if (alternate != null) {
            queryOptions.put(VariantDBAdaptor.ALTERNATE, alternate);
        }
        if (studies != null) {
            queryOptions.put(VariantDBAdaptor.STUDIES, Arrays.asList(studies.split(",")));
        }
        if (!variantType.isEmpty()) {
            queryOptions.put(VariantDBAdaptor.TYPE, variantType);
        }
        if (!maf.isEmpty()) {
            queryOptions.put(VariantDBAdaptor.MAF, maf);
        }
        if (missingAlleles >= 0) {
            queryOptions.put(VariantDBAdaptor.MISSING_ALLELES, missingAlleles);
        }
        if (missingGenotypes >= 0) {
            queryOptions.put(VariantDBAdaptor.MISSING_GENOTYPES, missingGenotypes);
        }

        return createOkResponse(variantMongoDbAdaptor.getAllVariantsByGene(geneId, queryOptions));
    }
    
    @GET
    @Path("/ranking")
    @ApiOperation(httpMethod = "GET", value = "Retrieves gene ranking", response = QueryResponse.class)
    public Response genesRankingByVariantsNumber(@QueryParam("species") String species,
                                                 @DefaultValue("10") @QueryParam("limit") int limit,
                                                 @DefaultValue("desc") @QueryParam("sort") String sort,
                                                 @DefaultValue("") @QueryParam("type") String variantType)
            throws IllegalOpenCGACredentialsException, UnknownHostException, IOException {
        try {
            checkParams();
        } catch (VersionException | SpeciesException ex) {
            return createErrorResponse(ex.toString());
        }
        
        VariantDBAdaptor variantMongoDbAdaptor = DBAdaptorConnector.getVariantDBAdaptor(species);
        
        if (!variantType.isEmpty()) {
            queryOptions.put("type", variantType);
        }
        
        if (sort.equalsIgnoreCase("desc")) {
            return createOkResponse(variantMongoDbAdaptor.getMostAffectedGenes(limit, queryOptions));
        } else if (sort.equalsIgnoreCase("asc")) {
            return createOkResponse(variantMongoDbAdaptor.getLeastAffectedGenes(limit, queryOptions));
        } else {
            return createOkResponse("Sorting criteria must be 'desc' or 'asc'");
        }
    }
    
    
}
