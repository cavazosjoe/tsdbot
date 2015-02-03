package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.tsdtv.ShowNotFoundException;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVLibrary;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.util.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Joe on 1/12/2015.
 */
@Singleton
public class TSDTVKillServlet extends HttpServlet {

    @Inject
    private TSDTV tsdtv;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {}

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(tsdtv.getNowPlaying() != null) {
            String ip = ServletUtils.getIpAddress(req);
            if(ip.equalsIgnoreCase(tsdtv.getNowPlaying().getMovie().owner)) {
                tsdtv.kill(true);
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have permission to end what's playing");
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "There isn't a stream running");
        }
    }
}
