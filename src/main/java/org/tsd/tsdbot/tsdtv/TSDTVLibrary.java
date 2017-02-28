package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.model.FillerType;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.tsdtv.model.TSDTVFiller;
import org.tsd.tsdbot.tsdtv.model.TSDTVShow;
import org.tsd.tsdbot.util.TSDTVUtil;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;

import javax.inject.Named;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class TSDTVLibrary {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVLibrary.class);

    private final File libraryDirectory;
    private final Random random;

    @Inject
    public TSDTVLibrary(Random random, @Named("tsdtvLibrary") File libraryDirectory) {
        logger.info("Constructing TSDTV library in path {}...", libraryDirectory.getAbsolutePath());
        this.random = random;
        this.libraryDirectory = libraryDirectory;
    }

    public TreeMap<TSDTVShow, TreeSet<TSDTVEpisode>> getCatalog() {
        TreeMap<TSDTVShow, TreeSet<TSDTVEpisode>> catalog = new TreeMap<>();
        for(TSDTVShow show : getAllShows()) {
            catalog.put(show, show.getOrderedEpisodes());
        }
        return catalog;
    }

    public TreeSet<TSDTVShow> getOrderedShows() {
        return Arrays.stream(libraryDirectory.listFiles())
                .filter(file -> file.isDirectory() && !isDotFile(file))
                .map(TSDTVShow::new)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public List<TSDTVShow> getAllShows() {
        return Arrays.stream(libraryDirectory.listFiles())
                .filter(file -> file.isDirectory() && !isDotFile(file))
                .map(TSDTVShow::new)
                .collect(Collectors.toList());
    }

    public TSDTVShow getShow(String query) throws ShowNotFoundException {
        List<TSDTVShow> matchingDirs = FuzzyLogic.fuzzySubset(
                query,
                getAllShows(),
                TSDTVShow::getRawName);

        if(matchingDirs.size() == 0) {
            throw new ShowNotFoundException("Could not find show matching \"" + query + "\"");
        } else if(matchingDirs.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Found multiple shows matching \"").append(query).append("\":");
            for(TSDTVShow show : matchingDirs) {
                sb.append(" ").append(show.getRawName());
            }
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

    private static boolean isDotFile(File f) {
        return f.getName().startsWith(".");
    }

}
