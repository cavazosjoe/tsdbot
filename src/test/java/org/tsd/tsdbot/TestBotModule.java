package org.tsd.tsdbot;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.tsd.tsdbot.config.ArchivistConfig;
import org.tsd.tsdbot.config.TSDBotConfiguration;
import org.tsd.tsdbot.module.AllChannels;
import org.tsd.tsdbot.module.BotOwner;
import org.tsd.tsdbot.module.MainChannel;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.stats.Stats;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TestBotModule extends AbstractModule {

    private final List allChannels = new LinkedList<>();
    private final String mainChannel;
    private File loggingPropertiesFile = null;

    @SuppressWarnings("unchecked")
    public TestBotModule(String mainChannel, String... channels) {
        this.mainChannel = mainChannel;
        this.allChannels.add(mainChannel);
        if(channels != null) {
            allChannels.addAll(Arrays.asList(channels));
        }
        try {
            loggingPropertiesFile = Files.createTempFile("hurr", "durr").toFile();
        } catch (Exception e) {

        }
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("serverUrl"))
                .toInstance(IntegTestUtils.SERVER_URL);
        bind(String.class).annotatedWith(MainChannel.class)
                .toInstance(mainChannel);
        bind(List.class).annotatedWith(AllChannels.class)
                .toInstance(allChannels);
        bind(HttpClient.class)
                .toInstance(HttpClients.createMinimal());
        bind(File.class).annotatedWith(Names.named("loggingProperties"))
                .toInstance(loggingPropertiesFile);
        Multibinder.newSetBinder(binder(), Stats.class);
        bind(ThreadManager.class).toInstance(new ThreadManager(10));
        bind(String.class).annotatedWith(BotOwner.class).toInstance(IntegTestUtils.BOT_OWNER);

        bind(TSDBotConfiguration.class).toInstance(buildTestConfig());
    }

    private TSDBotConfiguration buildTestConfig() {
        TSDBotConfiguration configuration = new TSDBotConfiguration();
        ArchivistConfig archivistConfig = new ArchivistConfig();
        try {
            archivistConfig.logsDir = Files.createTempDirectory("arch").toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create tempporary archivist directory");
        }
        configuration.archivist = archivistConfig;
        return configuration;
    }
}
