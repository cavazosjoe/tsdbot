package org.tsd.tsdbot.haloapi;

import com.google.inject.name.Names;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.Reflections;
import org.tsd.tsdbot.haloapi.model.metadata.HaloMeta;
import org.tsd.tsdbot.haloapi.model.metadata.Metadata;
import org.tsd.tsdbot.module.HttpModule;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
@Ignore
public class ApiClientTest {

    private static final ExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    @Test
    public void testGetMetadataCollection(HaloApiClient haloApiClient) throws Exception {
        Reflections reflections = new Reflections("org.tsd.tsdbot.haloapi.model.metadata");
        List<Class<? extends Metadata>> classesToTest = new LinkedList<>();
        for(Class<?> c : reflections.getTypesAnnotatedWith(HaloMeta.class)) {
            if (c.getAnnotation(HaloMeta.class).list()) {
                classesToTest.add((Class<? extends Metadata>) c);
            }
        }

        Metadata[] result;
        for(Class<? extends Metadata> clazz : classesToTest) {
            result = haloApiClient.getMetadataCollection(clazz);
            assertNotNull(result);
            assertTrue(result.length > 0);
        }
    }

    @AfterClass
    public static void shutdown() {
        executorService.shutdownNow();
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new HttpModule(executorService));
            bind(String.class).annotatedWith(Names.named("haloApiKey")).toInstance("3e93fc8f1bd9425aa9847cb833713cff");
        }
    }

}
