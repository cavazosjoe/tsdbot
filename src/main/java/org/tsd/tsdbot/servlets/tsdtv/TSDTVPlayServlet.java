package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.tsdtv.ShowNotFoundException;
import org.tsd.tsdbot.tsdtv.TSDTV;

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
