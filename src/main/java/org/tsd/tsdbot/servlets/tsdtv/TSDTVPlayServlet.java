package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.tsdtv.*;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.util.ServletUtils;
import org.tsd.tsdbot.util.SurgeProtector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class TSDTVPlayServlet extends HttpServlet {

    @Inject
    private TSDTV tsdtv;

    @Inject
    private TSDTVLibrary library;

    private final Set<String> servletBlacklist = new HashSet<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {}

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = req.getParameter("fileName");
        String show = req.getParameter("show");
        InetAddress inetAddress = null;
        try {
            TSDTVEpisode episode = library.getShow(show).getEpisode(fileName);
            inetAddress = InetAddress.getByName(ServletUtils.getIpAddress(req));
            if(!servletBlacklist.contains(inetAddress.toString())) {
                tsdtv.playFromWeb(episode, inetAddress);
            }
        } catch (ShowNotFoundException snfe) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find file matching show " + show + " and episode " + fileName);
        } catch (StreamLockedException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The stream is currently locked from web access");
        } catch (SurgeProtector.FloodException fe) {
            servletBlacklist.add(inetAddress.toString());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Flood detected. Calm down man");
        } catch (EpisodeAlreadyQueuedException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "That episode is already in the queue");
        }
    }
}
