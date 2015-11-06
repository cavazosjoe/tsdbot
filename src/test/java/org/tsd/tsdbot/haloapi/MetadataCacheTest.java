package org.tsd.tsdbot.haloapi;

import com.google.inject.name.Names;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.haloapi.model.metadata.FlexibleStatMeta;
import org.tsd.tsdbot.module.HttpModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.Assert.assertNotNull;

@RunWith(JukitoRunner.class)
public class MetadataCacheTest {

    private static final ExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    @Test
    public void testGetMetadata(MetadataCache cache) throws Exception {
        FlexibleStatMeta flexibleStatMeta
                = cache.getMetadata(FlexibleStatMeta.class, "ba327946-418c-495d-90c0-89f084bf7447");
        assertNotNull(flexibleStatMeta);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new HttpModule(executorService));
            bind(String.class).annotatedWith(Names.named("haloApiKey")).toInstance("3e93fc8f1bd9425aa9847cb833713cff");
        }
    }
}
