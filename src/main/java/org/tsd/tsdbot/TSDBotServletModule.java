package org.tsd.tsdbot;

import com.google.inject.servlet.ServletModule;
import org.tsd.tsdbot.servlets.StatusServlet;
import org.tsd.tsdbot.servlets.TestServlet;
import org.tsd.tsdbot.servlets.hustle.HustleChartServlet;
import org.tsd.tsdbot.servlets.hustle.HustleServlet;

/**
 * Created by Joe on 1/11/2015.
 */
public class TSDBotServletModule extends ServletModule {
    @Override
    protected void configureServlets() {

        bind(TestServlet.class);
        serve("/test").with(TestServlet.class);

        bind(HustleServlet.class);
        serve("/hustle").with(HustleServlet.class);
        bind(HustleChartServlet.class);
        serve("/hustle/chart").with(HustleChartServlet.class);
        bind(StatusServlet.class);
        serve("/status").with(StatusServlet.class);

    }
}
