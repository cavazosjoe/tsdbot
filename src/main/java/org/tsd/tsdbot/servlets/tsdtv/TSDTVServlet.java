package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVLibrary;
import org.tsd.tsdbot.util.ServletUtils;
import org.tsd.tsdbot.util.TSDTVUtil;

import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Servlet that backs the TSDTV player page
 */
@Singleton
public class TSDTVServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVServlet.class);

    @Inject
    private TSDTVLibrary library;

    @Inject @Named("tsdtvDirect")
    private String tsdtvDirectLink;

    @Inject @Named("videoFmt")
    private String videoFmt;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("catalog", library.getCatalog());
        req.setAttribute("directLink", tsdtvDirectLink);
        req.setAttribute("videoFmt", videoFmt);
        req.getRequestDispatcher("/tsdtv.jsp").forward(req, resp);
    }
}
