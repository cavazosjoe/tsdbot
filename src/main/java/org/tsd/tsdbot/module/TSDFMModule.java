package org.tsd.tsdbot.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.config.TSDFMConfig;
import org.tsd.tsdbot.tsdfm.TSDFM;
import org.tsd.tsdbot.tsdfm.TSDFMLibrary;

import java.io.File;

public class TSDFMModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(TSDFMModule.class);

    private final TSDFMConfig config;

    public TSDFMModule(TSDFMConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {

        bind(TSDFMConfig.class).toInstance(config);

        bind(String.class).annotatedWith(Names.named("tsdfmLog")).toInstance(config.logFile);
        log.info("Bound TSDFM log file to {}", config.logFile);

        File tsdfmLibrary = new File(config.library);
        bind(File.class)
            .annotatedWith(Names.named("tsdfmLibrary"))
            .toInstance(tsdfmLibrary);
        log.info("Bound TSDFM library directory to {}", tsdfmLibrary.getAbsolutePath());

        bind(String.class).annotatedWith(Names.named("tsdfmOut")).toInstance(config.target);
        log.info("Bound TSDFM target to {}", config.target);

        log.info("Binding TSDFM library...");
        bind(TSDFMLibrary.class).asEagerSingleton();

        log.info("Binding TSDFM client...");
        bind(TSDFM.class).asEagerSingleton();
    }
}
