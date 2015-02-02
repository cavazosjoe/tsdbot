package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.model.FillerType;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.tsdtv.model.TSDTVFiller;
import org.tsd.tsdbot.tsdtv.model.TSDTVShow;
import org.tsd.tsdbot.util.FuzzyLogic;
import org.tsd.tsdbot.util.TSDTVUtil;

import javax.inject.Named;
import java.io.File;
import java.util.*;

/**
 * Created by Joe on 2/1/2015.
 */
@Singleton
public class TSDTVLibrary {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVLibrary.class);

    @Inject @Named("tsdtvLibrary")
    private File libraryDirectory;

    @Inject
    private Random random;

    public TreeMap<TSDTVShow, TreeSet<TSDTVEpisode>> getCatalog() {
        TreeMap<TSDTVShow, TreeSet<TSDTVEpisode>> catalog = new TreeMap<>();
        for(TSDTVShow show : getAllShows()) {
            catalog.put(show, show.getOrderedEpisodes());
        }
        return catalog;
    }

    public TreeSet<TSDTVShow> getOrderedShows() {
        TreeSet<TSDTVShow> result = new TreeSet<>();
        for(File f : libraryDirectory.listFiles()) {
            if(f.isDirectory() && !f.getName().startsWith(".")) {
                result.add(new TSDTVShow(f));
            }
        }
        return result;
    }

    public LinkedList<TSDTVShow> getAllShows() {
        LinkedList<TSDTVShow> result = new LinkedList<>();
        for(File f : libraryDirectory.listFiles()) {
            if(f.isDirectory() && !f.getName().startsWith(".")) {
                result.add(new TSDTVShow(f));
            }
        }
        return result;
    }

    public TSDTVShow getShow(String query) throws ShowNotFoundException {
        List<TSDTVShow> matchingDirs = FuzzyLogic.fuzzySubset(
                query,
                getAllShows(),
                new FuzzyLogic.FuzzyVisitor<TSDTVShow>() {
                    @Override
                    public String visit(TSDTVShow o1) {
                        return o1.getRawName();
                    }
                });

        if(matchingDirs.size() == 0)
            throw new ShowNotFoundException("Could not find show matching \"" + query + "\"");
        else if(matchingDirs.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Found multiple shows matching \"").append(query).append("\":");
            for(TSDTVShow show : matchingDirs)
                sb.append(" ").append(show.getRawName());
            throw new ShowNotFoundException(sb.toString());
        } else {
            return matchingDirs.get(0);
        }
    }

    public TSDTVFiller getFiller(FillerType fillerType, String varId) {
        String searchingDirPath = libraryDirectory.getAbsolutePath();
        switch(fillerType) {
            case bump: {
                searchingDirPath += "/.bumps";
                break;
            }
            case commercial: {
                searchingDirPath += "/.commercials";
                break;
            }
            case show_intro: {
                // varId = raw show name (e.g. Hajime_no_Ippo)
                searchingDirPath = searchingDirPath + "/" + varId + "/" + TSDTVConstants.INTRO_DIR_NAME;
                break;
            }
            case block_intro: {
                // varId = block ID (e.g. ww)
                searchingDirPath = searchingDirPath + "/"
                        + TSDTVConstants.BLOCKS_DIR_NAME + "/" + varId + "/" + TSDTVConstants.INTRO_DIR_NAME;
                break;
            }
            case block_outro: {
                // varId = block ID (e.g. ww)
                searchingDirPath = searchingDirPath + "/"
                        + TSDTVConstants.BLOCKS_DIR_NAME + "/" + varId + "/" + TSDTVConstants.OUTRO_DIR_NAME;
                break;
            }
        }
        File video = TSDTVUtil.getRandomFileFromDirectory(random, new File(searchingDirPath));
        return (video != null) ? new TSDTVFiller(video, fillerType) : null;
    }

}
