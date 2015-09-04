package org.tsd.tsdbot.module;

import com.google.inject.servlet.ServletModule;
import org.tsd.tsdbot.servlets.PrintoutServlet;
import org.tsd.tsdbot.servlets.StatusServlet;
import org.tsd.tsdbot.servlets.hustle.HustleChartServlet;
import org.tsd.tsdbot.servlets.hustle.HustleServlet;
import org.tsd.tsdbot.servlets.tsdtv.*;

/**
 * Created by Joe on 1/11/2015.
 */
public class TSDBotServletModule extends ServletModule {
    @Override
    protected void configureServlets() {

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


        /**
         * Printouts
         */
        bind(PrintoutServlet.class);
        serve("/printouts/*").with(PrintoutServlet.class);

        /**
         * Status
         */
        bind(StatusServlet.class);
        serve("/status").with(StatusServlet.class);

    }
}
