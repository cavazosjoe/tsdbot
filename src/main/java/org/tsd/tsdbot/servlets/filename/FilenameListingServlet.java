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
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
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

        List<EncodedFilename> encodedFilenames = new LinkedList<>();
        for(File f : filenameLibrary.readFiles()) {
            encodedFilenames.add(new EncodedFilename(f.getName()));
        }

        req.setAttribute("filenames", encodedFilenames);
        req.setAttribute("serverUrl", serverUrl);
        req.setAttribute("name", tsdNames[random.nextInt(tsdNames.length)]);
        req.getRequestDispatcher("/filenames.jsp").forward(req, resp);
    }

    public static class EncodedFilename {
        private String encoded;
        private String unencoded;

        public EncodedFilename(String unencoded) throws UnsupportedEncodingException {
            this.unencoded = unencoded;
            this.encoded = URLEncoder.encode(unencoded, "UTF-8");
        }

        public String getEncoded() {
            return encoded;
        }

        public String getUnencoded() {
            return unencoded;
        }
    }

    private static final String[] tsdNames = {
            "The Strong Dudes",
            "The Swole Dudes",
            "The Salty Dudes",
            "Two Stupid Dogs",
            "Tex's Stupid Dogs"
    };
}
