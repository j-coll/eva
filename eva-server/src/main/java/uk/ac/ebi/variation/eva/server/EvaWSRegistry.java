package uk.ac.ebi.variation.eva.server;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by imedina on 01/04/14.
 * @author imedina
 */
public class EvaWSRegistry extends ResourceConfig {

    public EvaWSRegistry() {
//        packages("uk.ac.ebi.variation.eva.server.ws");
        register(CORSResponseFilter.class);
    }

}
