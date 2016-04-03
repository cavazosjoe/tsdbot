package org.tsd.tsdbot.servlets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.RecapLibrary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Singleton
public class RecapServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(RecapServlet.class);

    @Inject
    private RecapLibrary recapLibrary;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("Fetching recap from path: {}", req.getPathInfo());
        String recapId = req.getPathInfo().split("/")[1];
        try{
            logger.info("Parsed recap id {}", recapId);
            byte[] data = recapLibrary.getRecap(recapId).getBytes();
            IOUtils.copy(new ByteArrayInputStream(data), resp.getOutputStream());
            resp.getOutputStream().close();
        } catch (FileNotFoundException fnfe) {
            logger.error("File not found, recap id = " + recapId, fnfe);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find recap with id " + recapId);
        } catch (Exception e) {
            logger.error("Unknown error finding recap with id = " + recapId, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not find recap due to unknown error: " + e.getMessage());
        }
    }
}
