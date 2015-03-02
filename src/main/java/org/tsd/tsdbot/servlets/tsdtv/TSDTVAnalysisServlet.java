package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.tsd.tsdbot.tsdtv.TSDTVFileProcessor;
import org.tsd.tsdbot.tsdtv.processor.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by Joe on 2/28/2015.
 */
@Singleton
public class TSDTVAnalysisServlet extends HttpServlet {

    @Inject
    private TSDTVFileProcessor fileProcessor;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String analysisId = req.getPathInfo().split("/")[1];
        if(StringUtils.isEmpty(analysisId)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not parse analysis id");
        } else {
            AnalysisCollection analyses = fileProcessor.getAnalysesForId(analysisId);
            if(analyses == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find analysis with id " + analysisId);
            } else if(analyses.getAnalyses().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Analysis for " + analyses.getFolder() + " is empty");
            } else {
                resp.getOutputStream().print(analyses.getTextOutput());
                resp.getOutputStream().close();
            }
        }
    }

}
