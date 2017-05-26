package org.tsd.tsdbot.module;

import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.servlets.PrintoutServlet;
import org.tsd.tsdbot.servlets.RecapServlet;
import org.tsd.tsdbot.servlets.StatusServlet;
import org.tsd.tsdbot.servlets.filename.FilenameImageServlet;
import org.tsd.tsdbot.servlets.filename.FilenameListingServlet;
import org.tsd.tsdbot.servlets.filename.RandomFilenameImageServlet;
import org.tsd.tsdbot.servlets.hustle.HustleChartServlet;
import org.tsd.tsdbot.servlets.hustle.HustleServlet;
import org.tsd.tsdbot.servlets.tsdtv.*;

public class TSDBotServletModule extends ServletModule {

    private static Logger log = LoggerFactory.getLogger(TSDBotServletModule.class);

    @Override
    protected void configureServlets() {

        log.info("Binding servlets...");

        /**
         * Hustle
         */
        bind(HustleServlet.class);
        serve("/hustle").with(HustleServlet.class);

        bind(HustleChartServlet.class);
        serve("/hustle/chart").with(HustleChartServlet.class);


        /**
         * TSDTV
         */
        bind(TSDTVServlet.class);
        serve("/tsdtv").with(TSDTVServlet.class);

        bind(TSDTVPlayServlet.class);
        serve("/tsdtv/play").with(TSDTVPlayServlet.class);

        bind(TSDTVControlServlet.class);
        serve("/tsdtv/control").with(TSDTVControlServlet.class);

        bind(TSDTVNowPlayingServlet.class);
        serve("/tsdtv/np").with(TSDTVNowPlayingServlet.class);

        bind(TSDTVAnalysisServlet.class);
        serve("/tsdtv/analyzer/*").with(TSDTVAnalysisServlet.class);


        bind(PrintoutServlet.class);
        serve("/printouts/*").with(PrintoutServlet.class);

        bind(RecapServlet.class);
        serve("/recaps/*").with(RecapServlet.class);

        bind(FilenameListingServlet.class);
        serve("/filenames").with(FilenameListingServlet.class);

        bind(FilenameImageServlet.class);
        serve("/filenames/*").with(FilenameImageServlet.class);

        bind(RandomFilenameImageServlet.class);
        serve("/randomFilenames/*").with(RandomFilenameImageServlet.class);

        /**
         * Status
         */
        bind(StatusServlet.class);
        serve("/status").with(StatusServlet.class);

        log.info("TSDBotServletModule.configure() successful");

    }
}
