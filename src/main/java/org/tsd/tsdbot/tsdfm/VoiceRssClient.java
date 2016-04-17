package org.tsd.tsdbot.tsdfm;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoiceRssClient {

    private static final Logger log = LoggerFactory.getLogger(VoiceRssClient.class);
    private static final String apiTarget = "http://api.voicerss.org/";

    private final HttpClient httpClient;
    private final String apiKey;

    @Inject
    public VoiceRssClient(HttpClient httpClient,
                          @Named("voiceRssApiKey") String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    public byte[] getSpeech(String text) throws Exception {
        log.info("Retrieving text to speech for: {}", text);
        URIBuilder uriBuilder = new URIBuilder(apiTarget);
        uriBuilder.addParameter("key", apiKey);
        uriBuilder.addParameter("src", text);
        uriBuilder.addParameter("hl", EN_GB);
        uriBuilder.addParameter("f", AUDIO);

        HttpEntity entity = null;
        try {
            HttpGet get = new HttpGet(uriBuilder.build());
            HttpResponse response = httpClient.execute(get);
            entity = response.getEntity();
            return EntityUtils.toByteArray(entity);
        } finally {
            if(entity != null) {
                EntityUtils.consumeQuietly(entity);
            }
        }
    }

    private static final String EN_GB = "en-gb";
    private static final String AUDIO = "44khz_8bit_mono";

}
