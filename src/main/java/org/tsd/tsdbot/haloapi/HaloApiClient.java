package org.tsd.tsdbot.haloapi;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.haloapi.model.metadata.HaloMeta;
import org.tsd.tsdbot.haloapi.model.metadata.Metadata;

import java.lang.reflect.Array;

public class HaloApiClient {

    private static final Logger log = LoggerFactory.getLogger(HaloApiClient.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.setVisibility(mapper.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    private final String apiKey;
    private final HttpClient httpClient;

    @Inject
    public HaloApiClient(HttpClient httpClient,
                         @Named("haloApiKey") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    public <T extends Metadata> T[] getMetadataCollection(Class<T> metadataClass) throws Exception {
        HaloMeta m = metadataClass.getAnnotation(HaloMeta.class);
        String path = m.path();
        T[] something = (T[]) Array.newInstance(metadataClass, 0);
        return (T[]) sendRequest(String.format(metadataListTarget, path), something.getClass());
    }

    public <T extends Metadata> T getMetadata(Class<T> metadataClass, String id) throws Exception {
        HaloMeta m = metadataClass.getAnnotation(HaloMeta.class);
        String path = m.path();
        return sendRequest(String.format(metadataSingleTarget, path, id), metadataClass);
    }

    private <T> T sendRequest(String url, Class<T> clazz) throws Exception {

        log.info("Sending Halo API request: {} <- {}", clazz.getName(), url);
        HttpGet get = get(url);
        HttpResponse response = httpClient.execute(get);
        String responseString = EntityUtils.toString(response.getEntity());
        log.debug("- response: {}", responseString);
        try {
            return mapper.readValue(responseString, clazz);
        } catch (JsonMappingException jme) {
            log.warn("JsonMappingException: {}", jme.getMessage());
            // check if this failed due to rate limiting
            ApiError error = mapper.readValue(responseString, ApiError.class);
            if(error.getStatusCode() == 429) {
                log.info("- rate limit exceeded. Sleeping for 10 seconds...");
                Thread.sleep(10 * 1000);
                response = httpClient.execute(get);
                responseString = EntityUtils.toString(response.getEntity());
                log.debug("- retry response: {}", responseString);
                return mapper.readValue(responseString, clazz);
            } else {
                throw jme;
            }
        }

    }

    private HttpGet get(String url) {
        HttpGet get = new HttpGet(url);
        get.addHeader("Ocp-Apim-Subscription-Key", apiKey);
        return get;
    }

    private static final String metadataListTarget = "https://www.haloapi.com/metadata/h5/metadata/%s";
    private static final String metadataSingleTarget = "https://www.haloapi.com/metadata/h5/metadata/%s/%s";

    static class ApiError {
        int statusCode;
        String message;

        public int getStatusCode() {
            return statusCode;
        }

        public String getMessage() {
            return message;
        }
    }

}
