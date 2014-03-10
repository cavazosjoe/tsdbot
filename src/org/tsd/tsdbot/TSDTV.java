package org.tsd.tsdbot;

import javafx.collections.ObservableMap;
import javafx.scene.media.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.runnable.TSDTVStream;
import org.tsd.tsdbot.util.IRCUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTV {

    private static Logger logger = LoggerFactory.getLogger(TSDTV.class);

    private TSDBot bot;
    
    private String scriptDir;
    private String catalogDir;

    private Thread runningStream;

    public TSDTV(TSDBot bot) throws IOException {
        this.bot = bot;

        Properties prop = new Properties();
        InputStream fis = TSDBotLauncher.class.getResourceAsStream("/tsdbot.properties");
        prop.load(fis);
        catalogDir = prop.getProperty("tsdtv.catalog");
        scriptDir = prop.getProperty("mainDir");
    }

    public void catalog(String requester, String subdir) throws Exception {
        File printingDir;
        if(subdir == null) printingDir = new File(catalogDir);
        else {
            printingDir = new File(catalogDir + "/" + subdir);
            if(!printingDir.exists())
                throw new Exception("Could not locate directory " + subdir);
        }

        boolean first = true;
        StringBuilder catalogBuilder = new StringBuilder();
        for(File f : printingDir.listFiles()) {
            if(!first)catalogBuilder.append(" -- ");
            catalogBuilder.append(f.getName());
            if(f.isDirectory()) catalogBuilder.append(" (DIR)");
            first = false;
        }

        bot.sendMessages(requester, IRCUtil.splitLongString(catalogBuilder.toString()));

    }

    public void play(String channel, String dir, String query) throws Exception {

        if(runningStream != null)
            throw new Exception("There is already a stream running");

        File searchingDir;
        if(dir == null) searchingDir = new File(catalogDir);
        else {
            searchingDir = new File(catalogDir + "/" + dir);
            if(!searchingDir.exists())
                throw new Exception("Could not find directory " + dir);
        }

        LinkedList<File> matchedFiles = new LinkedList<>();
        for(File f : searchingDir.listFiles()) {
            if(f.getName().toLowerCase().contains(query.toLowerCase()))
                matchedFiles.add(f);
        }

        if(matchedFiles.size() == 0) {
            throw new Exception("Could not find movie that matches " + query);
        } else if(matchedFiles.size() > 1) {
            StringBuilder ex = new StringBuilder();
            ex.append("Found multiple matching movies: ");
            for(File match : matchedFiles)
                ex.append(match.getName()).append(" ");
            throw new Exception(ex.toString());
        }

        Media m = new Media(matchedFiles.get(0).getAbsolutePath());
        ObservableMap<String,Object> om = m.getMetadata();
        String artist = (String) om.get("Artist");
        String title = (String) om.get("Title");

        runningStream = new Thread(new TSDTVStream(this, scriptDir, matchedFiles.get(0).getAbsolutePath()));
        runningStream.start();

        bot.broadcast("[TSDTV] NOW PLAYING: " + artist + " - " + title + " -- http://www.twitch.tv/tsd_irc");
    }

    public void finishStream() {
        runningStream = null;
    }
}
