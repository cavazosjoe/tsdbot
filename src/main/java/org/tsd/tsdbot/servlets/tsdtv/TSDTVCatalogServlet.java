package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.util.TSDTVUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Created by Joe on 1/12/2015.
 */
@Singleton
public class TSDTVCatalogServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVCatalogServlet.class);

    @Inject
    private TSDTV tsdtv;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        File catalogDir = tsdtv.getCatalogDir();

        TreeSet<File> fileList = new TreeSet<>(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if(o1.isDirectory() && !o2.isDirectory()) {
                    return -1;
                } else if(o2.isDirectory() && !o1.isDirectory()) {
                    return 1;
                } else {
                    if(o1.isDirectory()) { // both files are directories, compare alphabetically
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    } else try { // both files are videos, compare by parsed episode number
                        return Integer.compare(TSDTVUtil.getEpisodeNumberFromFilename(o1.getName()),
                                TSDTVUtil.getEpisodeNumberFromFilename(o2.getName()));
                    } catch (Exception e) {
                        logger.warn("Error parsing episode numbers while sorting files {} and {}, skipping...",
                                o1.getName(), o2.getName());
                    }
                }
                return 0;
            }
        });

        if(req.getParameter("show") == null) {
            // list shows (folders) and top-level videos
            for(File f : catalogDir.listFiles()) {
                if(!f.getName().startsWith("."))
                    fileList.add(f);
            }
        } else {
            String show = req.getParameter("show");
            File subDir = new File(catalogDir.getAbsolutePath() + "/" + show);
            if((!subDir.exists()) || (!subDir.isDirectory())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not find show matching \"" + show + "\"");
                return;
            }
            for (File f : subDir.listFiles()) {
                if(!f.getName().startsWith("."))
                    fileList.add(f);
            }
        }

        req.setAttribute("files", fileList);
        req.getRequestDispatcher("/tsdtvCatalog.jsp").forward(req, resp);

    }

}
