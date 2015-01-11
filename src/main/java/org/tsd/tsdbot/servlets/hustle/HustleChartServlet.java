package org.tsd.tsdbot.servlets.hustle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jfree.chart.ChartUtilities;
import org.tsd.tsdbot.stats.HustleStats;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Joe on 1/11/2015.
 */
@Singleton
public class HustleChartServlet extends HttpServlet {

    @Inject
    private HustleStats hustleStats;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("image/png");

        ChartUtilities.writeChartAsPNG(resp.getOutputStream(), hustleStats.generateChart(), 1000, 600);

        resp.getOutputStream().close();
    }
}
