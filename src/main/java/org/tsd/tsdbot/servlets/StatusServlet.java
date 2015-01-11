package org.tsd.tsdbot.servlets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.stats.Stats;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by Joe on 1/11/2015.
 */
@Singleton
public class StatusServlet extends HttpServlet {

    @Inject
    private Set<Stats> stats;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LinkedHashMap<String, Object> report = new LinkedHashMap<>();
        for(Stats s : stats) {
            report.putAll(s.getReport());
        }
        req.setAttribute("stats", report);
        req.getRequestDispatcher("/status.jsp").forward(req, resp);
    }
}
