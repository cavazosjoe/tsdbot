package org.tsd.tsdbot.servlets.hustle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.stats.SystemStats;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Joe on 1/11/2015.
 */
@Singleton
public class HustleServlet extends HttpServlet {

    @Inject
    SystemStats systemStats;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        systemStats.getReport();
        req.getRequestDispatcher("/hustle.jsp").forward(req, resp);
    }
}
