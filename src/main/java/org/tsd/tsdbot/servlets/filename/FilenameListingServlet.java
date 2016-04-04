package org.tsd.tsdbot.servlets.filename;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.FilenameLibrary;

import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;

@Singleton
public class FilenameListingServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(FilenameListingServlet.class);

    @Inject
    private Random random;

    @Inject @Named("serverUrl")
    private String serverUrl;

    @Inject
    private FilenameLibrary filenameLibrary;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("filenames", filenameLibrary.readFiles());
        req.setAttribute("serverUrl", serverUrl);
        req.setAttribute("name", tsdNames[random.nextInt(tsdNames.length)]);
        req.getRequestDispatcher("/filenames.jsp").forward(req, resp);
    }

    private static final String[] tsdNames = {
            "The Strong Dudes",
            "The Swole Dudes",
            "The Salty Dudes",
            "Two Stupid Dogs",
            "Tex's Stupid Dogs"
    };
}
