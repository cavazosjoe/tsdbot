package org.tsd.tsdbot.haloapi;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.haloapi.model.stats.arena.ArenaMatch;
import org.tsd.tsdbot.haloapi.model.stats.arena.ArenaServiceRecordSearch;
import org.tsd.tsdbot.haloapi.model.stats.warzone.WarzoneMatch;
import org.tsd.tsdbot.haloapi.model.stats.warzone.WarzoneServiceRecordSearch;
import org.tsd.tsdbot.module.HttpModule;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.Assert.*;

@RunWith(JukitoRunner.class)
public class ApiTest {

    private static final ExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    private static final String warzoneServiceRecordEndpoint = "https://www.haloapi.com/stats/h5/servicerecords/warzone?players=%s";
    private static final String arenaServiceRecordEndpoint = "https://www.haloapi.com/stats/h5/servicerecords/arena?players=%s";
    private static final String warzoneCarnageEndpoint = "https://www.haloapi.com/stats/h5/warzone/matches/%s";
    private static final String arenaCarnageEndpoint = "https://www.haloapi.com/stats/h5/arena/matches/%s";

    @Test
    public void testWarzoneServiceRecord(HttpClient client, HttpGet get) throws Exception {
        get.setURI(new URI(String.format(warzoneServiceRecordEndpoint, "Schooly%20D")));
        HttpResponse response = client.execute(get);
        WarzoneServiceRecordSearch search = objectMapper.readValue(EntityUtils.toByteArray(response.getEntity()), WarzoneServiceRecordSearch.class);
        assertNotNull(search);
        assertEquals(1, search.getResults().size());
        assertEquals("Schooly D", search.getResults().get(0).getId());
    }

    @Test
    public void testArenaServiceRecord(HttpClient client, HttpGet get) throws Exception {
        get.setURI(new URI(String.format(arenaServiceRecordEndpoint, "Schooly%20D")));
        HttpResponse response = client.execute(get);
        ArenaServiceRecordSearch search = objectMapper.readValue(EntityUtils.toByteArray(response.getEntity()), ArenaServiceRecordSearch.class);
        assertNotNull(search);
        assertEquals(1, search.getResults().size());
        assertEquals("Schooly D", search.getResults().get(0).getId());
    }

    @Test
    public void testWarzoneCarnageReport(HttpClient client, HttpGet get) throws Exception {
        get.setURI(new URI(String.format(warzoneCarnageEndpoint, "ee2db08e-1c71-4a78-9155-899dae2bec81")));
        HttpResponse response = client.execute(get);
        WarzoneMatch match = objectMapper.readValue(EntityUtils.toByteArray(response.getEntity()), WarzoneMatch.class);
        assertNotNull(match);
        assertEquals(25, match.getPlayerStats().size());
        assertEquals(2, match.getTeamStats().size());
        assertTrue(match.isMatchOver());
        assertEquals("PT6M28.6634683S", match.getTotalDuration());
    }

    @Test
    public void testArenaCarnageReport(HttpClient client, HttpGet get) throws Exception {
        get.setURI(new URI(String.format(arenaCarnageEndpoint, "48502809-f796-4213-927c-2690a27d9fd3")));
        HttpResponse response = client.execute(get);
        ArenaMatch match = objectMapper.readValue(EntityUtils.toByteArray(response.getEntity()), ArenaMatch.class);
        assertNotNull(match);
        assertEquals(8, match.getPlayerStats().size());
        assertEquals(2, match.getTeamStats().size());
        assertTrue(match.isMatchOver());
        assertEquals("PT3M38.7341149S", match.getTotalDuration());
    }

    @AfterClass
    public static void shutdown() {
        executorService.shutdownNow();
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new HttpModule(executorService));
            bind(HttpGet.class).toProvider(ApiCallProvider.class);
        }
    }

    static class ApiCallProvider implements Provider<HttpGet> {
        @Override
        public HttpGet get() {
            HttpGet get = new HttpGet();
            get.addHeader("Ocp-Apim-Subscription-Key", "3e93fc8f1bd9425aa9847cb833713cff");
            return get;
        }
    }
}
