package org.tsd.tsdbot.servlets.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.*;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.tsdtv.model.TSDTVFiller;
import org.tsd.tsdbot.tsdtv.model.TSDTVShow;
import org.tsd.tsdbot.util.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Servlet that returns queue/schedule info for AJAX requests
 */
@Singleton
public class TSDTVNowPlayingServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVNowPlayingServlet.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm z");

    static {
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    @Inject
    private TSDTV tsdtv;

    @Inject
    private TSDTVLibrary library;

    @Inject
    private Scheduler scheduler;

    @Inject
    @Named("serverUrl")
    private String serverUrl;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        StringBuilder out = new StringBuilder();

        switch (req.getParameter("type")) {

            case "nowplaying": {
                if(tsdtv.getNowPlaying() != null) {
                    if(tsdtv.getNowPlaying().getMovie().video instanceof TSDTVEpisode) {
                        TSDTVEpisode episode = (TSDTVEpisode) tsdtv.getNowPlaying().getMovie().video;
                        String nowplayingItemTemplate = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("fragments/tsdtv/nowplayingItem.html"));
                        out.append(nowplayingItemTemplate
                                        .replaceAll("%%show_pic%%", serverUrl + "/tsdtv/np?type=queue_img&show=" + episode.getShow().getRawName())
                                        .replaceAll("%%show_name%%", episode.getShow().getPrettyName())
                                        .replaceAll("%%show_episode%%", episode.getPrettyName())
                        );
                        try {
                            if(tsdtv.authorized(new TSDTVWebUser(InetAddress.getByName(ServletUtils.getIpAddress(req))))) {
                                // add some controls if this user owns this stream
                                String controlButtonsTemplate = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("fragments/tsdtv/controlButtons.html"));
                                out.append(controlButtonsTemplate);
                            }
                        } catch (NoStreamRunningException nsre) {logger.error("NSRE", nsre);} // shouldn't happen
                    } else {
                        TSDTVFiller filler = (TSDTVFiller) tsdtv.getNowPlaying().getMovie().video;
                        out.append(filler.toString());
                    }
                } else {
                    out.append("<em>Nothing playing</em>");
                }
                break;
            }

            case "queue": {
                if(!tsdtv.getQueue().isEmpty()) try {
                    String queueItemTemplate = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("fragments/tsdtv/queueItem.html"));
                    for (TSDTVQueueItem program : tsdtv.getQueue()) {
                        if (program.video.isBroadcastable()) {
                            TSDTVEpisode episode = (TSDTVEpisode) program.video;
                            out.append(queueItemTemplate
                                            .replaceAll("%%show_pic%%", serverUrl + "/tsdtv/np?type=queue_img&show=" + episode.getShow().getRawName())
                                            .replaceAll("%%show_name%%", episode.getShow().getPrettyName())
                                            .replaceAll("%%show_time%%", sdf.format(program.startTime))
                                            .replaceAll("%%show_episode%%", episode.getPrettyName())
                            );
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error retrieving TSDTV queue", e);
                } else {
                    out.append("<em>Nothing in the queue</em>");
                }
                break;
            }

            case "blocks": {
                String blockItemTemplate = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("fragments/tsdtv/blockItem.html"));
                try {
                    TreeMap<Date, TSDTV.TSDTVBlock> remainingBlocks = tsdtv.getRemainingBlocks(true);
                    for (Date date : remainingBlocks.keySet()) {
                        TSDTV.TSDTVBlock block = remainingBlocks.get(date);
                        StringBuilder sb = new StringBuilder();
                        for(String show : block.scheduleParts) {
                            String prettyShow = show.replaceAll("_"," ");
                            if( (!show.startsWith(".")) && (!sb.toString().contains(prettyShow)) ) {
                                if (sb.length() > 0)
                                    sb.append(", ");
                                sb.append(prettyShow);
                            }
                        }
                        out.append(blockItemTemplate
                                        .replaceAll("%%block_name%%", block.name)
                                        .replaceAll("%%block_shows%%", sb.toString())
                                        .replaceAll("%%block_time%%", sdf.format(date))
                        );
                    }
                } catch (Exception e) {
                    out.append("Error retrieving scheduled blocks<br/>");
                }
                break;
            }

            case "viewers": {
                out.append(tsdtv.getViewerCount());
                break;
            }

            case "queue_img": {
                String showString = req.getParameter("show");
                resp.setContentType("image/jpg");
                try {
                    TSDTVShow show = library.getShow(showString);
                    File img = show.getQueueImage();
                    if(img == null || !img.exists())
                        throw new Exception("Could not find image for show " + showString);
                    IOUtils.copy(new FileInputStream(img), resp.getOutputStream());
                } catch (Exception e) {
                    IOUtils.copy(new URL("http://i.imgur.com/6HNzLdK.jpg").openStream(), resp.getOutputStream());
                } finally {
                    resp.getOutputStream().close();
                }
                return;
            }
        }

        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().print(out.toString());

    }

}
