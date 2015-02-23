package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.tsdtv.ShowNotFoundException;
import org.tsd.tsdbot.tsdtv.StreamLockedException;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVLibrary;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.util.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;

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
            InetAddress inetAddress = InetAddress.getByName(ServletUtils.getIpAddress(req));
            tsdtv.playFromWeb(episode, inetAddress);
        } catch (ShowNotFoundException snfe) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find file matching show " + show + " and episode " + fileName);
        } catch (StreamLockedException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The stream is currently locked from web access");
        }
    }
}
