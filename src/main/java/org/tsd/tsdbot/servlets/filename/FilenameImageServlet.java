package org.tsd.tsdbot.servlets.filename;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.FilenameLibrary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;

@Singleton
public class FilenameImageServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FilenameImageServlet.class);

    @Inject
    private FilenameLibrary filenameLibrary;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = URLDecoder.decode(req.getPathInfo(), "UTF-8");
        logger.info("Fetching filename from path: {}", path);
        String filename = null;
        try{
            filename = path.split("/")[1];
            logger.info("Parsed filename {}", filename);
            byte[] data = filenameLibrary.getFile(filename);
            IOUtils.copy(new ByteArrayInputStream(data), resp.getOutputStream());
            resp.getOutputStream().close();
        } catch (FileNotFoundException fnfe) {
            logger.error("File not found, filename = " + filename, fnfe);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find file with name " + filename);
        } catch (Exception e) {
            logger.error("Unknown error finding filename " + filename, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not find filename due to unknown error: " + e.getMessage());
        }
    }
}
