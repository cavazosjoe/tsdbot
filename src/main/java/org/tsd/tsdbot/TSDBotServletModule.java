package org.tsd.tsdbot;

import com.google.inject.servlet.ServletModule;
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

        bind(TSDTVNowPlayingServlet.class);
        serve("/tsdtv/np").with(TSDTVNowPlayingServlet.class);


        /**
         * Status
         */
        bind(StatusServlet.class);
        serve("/status").with(StatusServlet.class);

    }
}
