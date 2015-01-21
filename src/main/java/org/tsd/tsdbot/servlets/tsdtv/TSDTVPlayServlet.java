package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.tsdtv.ShowNotFoundException;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVProgram;
import org.tsd.tsdbot.tsdtv.TSDTVStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by Joe on 1/12/2015.
 */
@Singleton
public class TSDTVPlayServlet extends HttpServlet {

    @Inject
    private TSDTV tsdtv;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // queue with dotshows (commercials, bumps, intros, etc) removed
        LinkedList<TSDTVProgram> effectiveQueue = new LinkedList<>();

        if(tsdtv.getNowPlaying() != null && !tsdtv.getNowPlaying().getMovie().show.startsWith(".")) {
            effectiveQueue.add(tsdtv.getNowPlaying().getMovie());
        }

        for(TSDTVProgram prog : tsdtv.getQueue()) {
            if(!prog.show.startsWith("."))
                effectiveQueue.add(prog);
        }
        req.setAttribute("queue", effectiveQueue);
        req.getRequestDispatcher("/tsdtvPlayResult.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = req.getParameter("fileName");
        String show = req.getParameter("show");
        if(fileName != null) try {
            boolean playingNow = tsdtv.playFromCatalog(fileName, show);
            resp.sendRedirect("/tsdtv/play?result=" + playingNow);
        } catch (ShowNotFoundException snfe) {
            resp.sendRedirect("/tsdtv/play?result=error");
        } else {
            resp.sendRedirect("/tsdtv/play?result=unknown");
        }
    }
}
