package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.tsdtv.NoStreamRunningException;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVWebUser;
import org.tsd.tsdbot.util.ServletUtils;

import javax.naming.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;

@Singleton
public class TSDTVControlServlet extends HttpServlet {

    @Inject
    private TSDTV tsdtv;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {}

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            InetAddress inetAddress = InetAddress.getByName(ServletUtils.getIpAddress(req));
            TSDTVWebUser tsdtvWebUser = new TSDTVWebUser(inetAddress);
            if(req.getParameter("type").equals("kill")) {
                tsdtv.kill(tsdtvWebUser);
            } else if (req.getParameter("type").equals("pause")) {
                switch (tsdtv.getState()) {
                    case running: {
                        tsdtv.pause(tsdtvWebUser);
                        break;
                    }
                    case paused: {
                        tsdtv.unpause(tsdtvWebUser);
                        break;
                    }
                    default:
                        throw new IllegalStateException("Trying to pause or unpause a " + tsdtv.getState() + " stream");
                }
            }
        } catch (NoStreamRunningException nsre) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "There isn't a stream running");
        } catch (AuthenticationException ae) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have permission to perform that action");
        } catch (IllegalStateException ise) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, ise.getMessage());
        }
    }
}
