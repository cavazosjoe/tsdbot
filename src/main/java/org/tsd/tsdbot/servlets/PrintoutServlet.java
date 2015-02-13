package org.tsd.tsdbot.servlets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.PrintoutLibrary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Joe on 2/6/2015.
 */
@Singleton
public class PrintoutServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(PrintoutServlet.class);

    @Inject
    private PrintoutLibrary printoutLibrary;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String printoutId = req.getPathInfo().split("/")[1];
        try{
            byte[] data = printoutLibrary.getPrintout(printoutId);
            IOUtils.copy(new ByteArrayInputStream(data), resp.getOutputStream());
            resp.getOutputStream().close();
        } catch (FileNotFoundException fnfe) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find printout with id " + printoutId);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not find printout due to unknown error: " + e.getMessage());
        }
    }
}
