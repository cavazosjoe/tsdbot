package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.tsdtv.*;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Joe on 1/12/2015.
 */
@Singleton
public class TSDTVPlayServlet extends HttpServlet {

    @Inject
    private TSDTV tsdtv;

    @Inject
    private TSDTVLibrary library;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {}

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = req.getParameter("fileName");
        String show = req.getParameter("show");
        try {
            TSDTVEpisode episode = library.getShow(show).getEpisode(fileName);
            tsdtv.playFromCatalog(episode);
        } catch (ShowNotFoundException snfe) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find file matching show " + show + " and episode " + fileName);
        }
    }
}
