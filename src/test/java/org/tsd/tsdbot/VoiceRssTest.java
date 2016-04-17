package org.tsd.tsdbot;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

@RunWith(MockitoJUnitRunner.class)
public class VoiceRssTest {

    private static final String apiTarget = "http://api.voicerss.org/";

    @Test
    public void test() {

        try(CloseableHttpClient client = HttpClients.createMinimal()) {

            URIBuilder uriBuilder = new URIBuilder(apiTarget);
            uriBuilder.addParameter("key", "71adbe92a6b740f188579d852cee39ef");
            uriBuilder.addParameter("src", "You're tuned into TSDFM: the only radio station where I can do whatever " +
                    "I want, whenever I want, and there's nothing you can do about it");
            uriBuilder.addParameter("hl", "en-gb");
            uriBuilder.addParameter("f", "44khz_8bit_mono");

            HttpGet get = new HttpGet(uriBuilder.build());
            HttpResponse response = client.execute(get);
            byte[] bytes = EntityUtils.toByteArray(response.getEntity());
            IOUtils.copy(new ByteArrayInputStream(bytes), new FileOutputStream(new File("C:/Users/Joe/Desktop/speech.mp3")));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

    }
}
