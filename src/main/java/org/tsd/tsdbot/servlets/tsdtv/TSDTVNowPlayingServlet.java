package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.quartz.Scheduler;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVProgram;
import org.tsd.tsdbot.tsdtv.TSDTVStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Servlet that returns queue/schedule info for AJAX requests
 */
@Singleton
public class TSDTVNowPlayingServlet extends HttpServlet {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm z");

    static {
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    @Inject
    private TSDTV tsdtv;

    @Inject
    private Scheduler scheduler;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        StringBuilder out = new StringBuilder();

        TSDTVStream nowPlaying = tsdtv.getNowPlaying();
        if(nowPlaying != null) {
            if(nowPlaying.getMovie().block != null) {
                out.append(formatBlock(nowPlaying.getMovie().block.name));
                out.append("<br/>");
            }
            out.append("<strong>NP - ").append(nowPlaying.getMovie().toPrettyString()).append("</strong>");
            out.append("<br/>");
        }

        for(TSDTVProgram program : tsdtv.getQueue()) {
            out.append(sdf.format(program.startTime)).append(" - ").append(program.toPrettyString());
            out.append("<br/>");
        }

        try {
            TreeMap<Date, TSDTV.TSDTVBlock> remainingBlocks = tsdtv.getRemainingBlocks(true);
            for(Date date : remainingBlocks.keySet()) {
                TSDTV.TSDTVBlock block = remainingBlocks.get(date);
                out.append(formatBlock(sdf.format(date) + ": " + block.name));
                out.append("<br/>");
            }
        } catch (Exception e) {
            out.append("Error retrieving scheduled blocks<br/>");
        }

        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().print(out.toString());
    }

    private String formatBlock(String blockName) {
        return "<span style=\"font-size: 20px; text-align: center;\">" + blockName + "</span>";
    }
}
