package org.tsd.tsdbot.haloapi;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.tsd.tsdbot.haloapi.model.metadata.HaloMeta;

import java.util.List;

public class HaloApiClient {

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

    public <T> List<T> getMetadataList(Class<T> metadataClass) throws Exception {
        HaloMeta m = metadataClass.getAnnotation(HaloMeta.class);
        String path = m.path();
        HttpResponse response = httpClient.execute(get(String.format(metadataListTarget, path)));
        return mapper.readValue(EntityUtils.toString(response.getEntity()), TypeFactory.defaultInstance().constructCollectionType(List.class, metadataClass));
    }

    public <T> T getMetadataSingle(Class<T> metadataClass, String id) throws Exception {
        HaloMeta m = metadataClass.getAnnotation(HaloMeta.class);
        String path = m.path();
        HttpResponse response = httpClient.execute(get(String.format(metadataSingleTarget, path, id)));
        return mapper.readValue(EntityUtils.toString(response.getEntity()), metadataClass);
    }

    private HttpGet get(String url) {
        HttpGet get = new HttpGet(url);
        get.addHeader("Ocp-Apim-Subscription-Key", "3e93fc8f1bd9425aa9847cb833713cff");
        return get;
    }

    private static final String metadataListTarget = "https://www.haloapi.com/metadata/h5/metadata/%s";
    private static final String metadataSingleTarget = "https://www.haloapi.com/metadata/h5/metadata/%s/%s";

}
