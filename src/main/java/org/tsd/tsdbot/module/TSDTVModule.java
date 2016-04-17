package org.tsd.tsdbot.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.config.TSDTVConfig;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVFileProcessor;
import org.tsd.tsdbot.tsdtv.TSDTVLibrary;

import java.io.File;

public class TSDTVModule extends AbstractModule {
    
    private static final Logger log = LoggerFactory.getLogger(TSDTVModule.class);
    
    private final TSDTVConfig tsdtvConfig;

    public TSDTVModule(TSDTVConfig tsdtvConfig) {
        this.tsdtvConfig = tsdtvConfig;
    }

    @Override
    protected void configure() {
        // arguments to ffmpeg commands
        bind(String.class)
            .annotatedWith(Names.named("ffmpegArgs"))
            .toInstance(tsdtvConfig.ffmpegArgs);
        log.info("Bound ffmpeg arguments to {}", tsdtvConfig.ffmpegArgs);

        // output of ffmpeg commands
        bind(String.class)
            .annotatedWith(Names.named("ffmpegOut"))
            .toInstance(tsdtvConfig.ffmpegOut);
        log.info("Bound ffmpeg output to {}", tsdtvConfig.ffmpegOut);

        // direct link to TSDTV stream (to be opened in video players)
        bind(String.class)
            .annotatedWith(Names.named("tsdtvDirect"))
            .toInstance(tsdtvConfig.directLink);
        log.info("Bound TSDTV direct link to {}", tsdtvConfig.directLink);

        // video format used by web player
        bind(String.class)
            .annotatedWith(Names.named("videoFmt"))
            .toInstance(tsdtvConfig.videoFmt);
        log.info("Bound ffmpeg video format to {}", tsdtvConfig.videoFmt);

        File tsdtvLibrary = new File(tsdtvConfig.catalog);
        bind(File.class)
            .annotatedWith(Names.named("tsdtvLibrary"))
            .toInstance(tsdtvLibrary);
        log.info("Bound TSDTV library directory to {}", tsdtvLibrary.getAbsolutePath());

        File tsdtvRaws = new File(tsdtvConfig.raws);
        bind(File.class)
            .annotatedWith(Names.named("tsdtvRaws"))
            .toInstance(tsdtvRaws);
        log.info("Bound TSDTV raws directory to {}", tsdtvRaws.getAbsolutePath());

        log.info("Binding TSDTV library...");
        bind(TSDTVLibrary.class).asEagerSingleton();

        log.info("Binding TSDTV file processor...");
        bind(TSDTVFileProcessor.class).asEagerSingleton();

        log.info("Binding TSDTV client...");
        bind(TSDTV.class).asEagerSingleton();
    }
}
