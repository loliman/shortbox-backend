package xyz.shortbox.backend.rest.util;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

/**
 * Strips all empty values from JSON Objects returned by restful methods.
 * <p>
 * Empty means null values, empty arrays and empty lists.
 * </p>
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonNotEmptyProvider extends JacksonJaxbJsonProvider {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
    }

    public JsonNotEmptyProvider() {
        super.setMapper(objectMapper);
    }
}