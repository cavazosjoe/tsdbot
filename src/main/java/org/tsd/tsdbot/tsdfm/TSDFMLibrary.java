package org.tsd.tsdbot.tsdfm;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.Persistable;
import org.tsd.tsdbot.tsdfm.model.TSDFMAlbum;
import org.tsd.tsdbot.tsdfm.model.TSDFMArtist;
import org.tsd.tsdbot.tsdfm.model.TSDFMSong;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;
import org.tsd.tsdbot.util.fuzzy.FuzzyVisitor;

import javax.inject.Named;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class TSDFMLibrary implements Persistable {

    private static final Logger log = LoggerFactory.getLogger(TSDFMLibrary.class);

    public static final Set<String> allowedFileTypes = new HashSet<>(Arrays.asList("mp3", "ogg", "wav"));

    private final DBConnectionProvider connectionProvider;
    private final File libraryDirectory;
    private final TreeSet<TSDFMArtist> artists = new TreeSet<>();
    private final Random random;

    @Inject
    public TSDFMLibrary(DBConnectionProvider connectionProvider,
                        Random random,
                        @Named("tsdfmLibrary") File libraryDirectory) {
        this.libraryDirectory = libraryDirectory;
        this.connectionProvider = connectionProvider;
        this.random = random;
        load();
    }

    @Override
    public void initDB() throws SQLException {
        log.info("Initializing TSDTV database");
        Connection connection = connectionProvider.get();

        // create song tag table if it doesn't exist
        try(PreparedStatement ps = connection.prepareStatement(CREATE_TAGS_TABLE)) {
            log.info("{}: {}", TAGS_TABLE, CREATE_TAGS_TABLE);
            ps.executeUpdate();
        }
    }

    public void load() {
        artists.clear();
        for(File artist : libraryDirectory.listFiles()) {
            if(artist.isDirectory()) {
                artists.add(new TSDFMArtist(artist));
            }
        }
        try {
            initDB();
        } catch (Exception e) {
            log.error("Error loading tsdfm database", e);
        }
    }

    String sanitizeTag(String tag) {
        return tag.trim();
    }

    public void tagSong(TSDFMSong song, Collection<String> tags) throws SQLException {
        String songPath = song.getMusicFile().getAbsolutePath();
        String countQuery;
        String insertStatement;
        Connection connection = connectionProvider.get();
        for(String tag : tags) {
            tag = sanitizeTag(tag);
            try(PreparedStatement ps = connection.prepareStatement(QUERY_EXISTING_TAG)) {
                ps.setString(1, songPath);
                ps.setString(2, tag);
                try (ResultSet result = ps.executeQuery()) {
                    result.next();
                    if (result.getInt(1) == 0) {
                        log.info("Could not find tag '{}' for song {}, adding...", tag, songPath);
                        try (PreparedStatement ps1 = connection.prepareCall(INSERT_TAG)) {
                            ps1.setString(1, songPath);
                            ps1.setString(2, tag);
                            ps1.executeUpdate();
                        }
                    } else {
                        log.warn("Tag {} already exists for file {} -- skipping...", tag, songPath);
                    }
                }
            }
        }
    }

    public void tagAlbum(TSDFMAlbum album, Collection<String> tags) throws SQLException {
        for(TSDFMSong song : album.getSongs()) {
            tagSong(song, tags);
        }
    }

    public void tagArtist(TSDFMArtist artist, Collection<String> tags) throws SQLException {
        for(TSDFMAlbum album : artist.getAlbums()) {
            tagAlbum(album, tags);
        }
        for(TSDFMSong song : artist.getSongsNoAlbum()) {
            tagSong(song, tags);
        }
    }

    public TSDFMArtist getArtist(String query) throws TSDFMQueryException {
        List<TSDFMArtist> matches = queryArtists(query);
        if(matches.size() > 1) {
            String s = matches.stream().map(TSDFMArtist::getName).collect(Collectors.joining(", "));
            throw new TSDFMQueryException(String.format("Found multiple artists matching \"%s\": %s", query, s));
        } else if(matches.size() == 0) {
            throw new TSDFMQueryException(String.format("Found no artists matching \"%s\"", query));
        } else {
            return matches.get(0);
        }
    }

    List<TSDFMArtist> queryArtists(String query) {
        if(StringUtils.isBlank(query))
            return new LinkedList<>(artists);

        return FuzzyLogic.fuzzySubset(query, artists, new FuzzyVisitor<TSDFMArtist>() {
            @Override
            public String visit(TSDFMArtist o1) {
                return o1.getName();
            }
        });
    }

    public TSDFMAlbum getAlbum(String albumQuery, String artistQuery) throws TSDFMQueryException {
        List<TSDFMArtist> artistsToSearch = new LinkedList<>();
        if(StringUtils.isBlank(artistQuery)) {
            artistsToSearch.addAll(artists);
        } else {
            artistsToSearch.addAll(queryArtists(artistQuery));
        }

        List<TSDFMAlbum> matches = queryAlbums(albumQuery, artistsToSearch);
        if(matches.size() > 1) {
            String s = matches.stream().map(TSDFMAlbum::getName).collect(Collectors.joining(", "));
            throw new TSDFMQueryException(String.format("Found multiple albums matching \"%s\": %s", albumQuery, s));
        } else if(matches.size() == 0) {
            throw new TSDFMQueryException(String.format("Found no albums matching \"%s\"", albumQuery));
        } else {
            return matches.get(0);
        }
    }

    List<TSDFMAlbum> queryAlbums(String query, Collection<TSDFMArtist> artistsToSearch) {

        // query is blank, return all albums for the selected artists
        if(StringUtils.isBlank(query)) {
            Set<TSDFMAlbum> allAlbums = new HashSet<>();
            for(TSDFMArtist artist : artistsToSearch) {
                allAlbums.addAll(artist.getAlbums());
            }
            return new LinkedList<>(allAlbums);
        }

        // query is not blank, perform a fuzzy search
        Set<TSDFMAlbum> albumsToSearch = new HashSet<>();
        for(TSDFMArtist artist : artistsToSearch) {
            albumsToSearch.addAll(artist.getAlbums());
        }

        return FuzzyLogic.fuzzySubset(query, albumsToSearch, new FuzzyVisitor<TSDFMAlbum>() {
            @Override
            public String visit(TSDFMAlbum o1) {
                return o1.getName();
            }
        });
    }

    public TSDFMSong getSong(String songQuery, String albumQuery, String artistQuery) throws TSDFMQueryException {
        List<TSDFMArtist> artistsToSearch = new LinkedList<>();
        if(StringUtils.isBlank(artistQuery)) {
            artistsToSearch.addAll(artists);
        } else {
            artistsToSearch.addAll(queryArtists(artistQuery));
        }

        Set<TSDFMSong> songs = new HashSet<>();
        for(TSDFMArtist artist : artistsToSearch) {
            if(StringUtils.isBlank(albumQuery)) {
                songs.addAll(artist.getAllSongs());
            } else {
                List<TSDFMAlbum> albumsToSearch = queryAlbums(albumQuery, Collections.singletonList(artist));
                for(TSDFMAlbum album : albumsToSearch) {
                    songs.addAll(album.getSongs());
                }
            }
        }

        List<TSDFMSong> matches = FuzzyLogic.fuzzySubset(songQuery, songs, new FuzzyVisitor<TSDFMSong>() {
            @Override
            public String visit(TSDFMSong o1) {
                return o1.getTitle();
            }
        });

        if(matches.size() > 1) {
            String s = matches.stream().map(TSDFMSong::getTitle).collect(Collectors.joining(", "));
            throw new TSDFMQueryException(String.format("Found multiple songs matching \"%s\": %s", songQuery, s));
        } else if(matches.size() == 0) {
            throw new TSDFMQueryException(String.format("Found no songs matching \"%s\"", songQuery));
        } else {
            return matches.get(0);
        }
    }

    Set<TSDFMSong> getAllSongs() {
        Set<TSDFMSong> allSongs = new HashSet<>();
        for(TSDFMArtist artist : queryArtists(null)) {
            allSongs.addAll(artist.getSongsNoAlbum());
            for(TSDFMAlbum album : artist.getAlbums()) {
                allSongs.addAll(album.getSongs());
            }
        }
        return allSongs;
    }

    public TSDFMSong getSongFromPath(String pathString) throws TSDFMQueryException {
        try {
            File pathAsFile = new File(pathString);
            if(!pathAsFile.exists()) {
                throw new TSDFMQueryException("File " + pathString + " does not exist");
            }
            Collection<TSDFMSong> matchingSongs = Collections2.filter(allSongsCache.get(allSongsCacheKey), new Predicate<TSDFMSong>() {
                @Override
                public boolean apply(TSDFMSong tsdfmSong) {
                    return pathAsFile.equals(tsdfmSong.getMusicFile());
                }
            });

            if (matchingSongs.size() == 0) {
                throw new TSDFMQueryException("Could not find any songs matching path " + pathString);
            }
            else if (matchingSongs.size() > 1) {
                log.error("Found multiple songs matching path {}: {}", pathString, StringUtils.join(matchingSongs, "||"));
                throw new TSDFMQueryException("Found multiple songs matching path " + pathString);
            }
            else {
                return matchingSongs.iterator().next();
            }
        } catch (ExecutionException ee) {
            throw new TSDFMQueryException("Error accessing song cache");
        }
    }

    public Set<TSDFMSong> getAllSongsForTags(Collection<String> tags) throws SQLException {
        Connection dbConn = connectionProvider.get();

        StringBuilder innerQueryBuilder = new StringBuilder();
        for(String tag : tags) {
            if(innerQueryBuilder.length() > 0) {
                innerQueryBuilder.append(",");
            }
            innerQueryBuilder.append("'").append(tag).append("'");
        }

        String query = "select path from TSDFM_TAGS where tag in (" + innerQueryBuilder.toString() + ")";
        Set<TSDFMSong> matchingSongs = new HashSet<>();
        try(PreparedStatement ps = dbConn.prepareStatement(query) ; ResultSet result = ps.executeQuery()) {
            while(result.next()) try {
                matchingSongs.add(getSongFromPath(result.getString(1)));
            } catch (TSDFMQueryException qe) {
                log.info("Query error while getting tagged songs: " + qe.getMessage());
            }
        }

        return matchingSongs;
    }

    public TSDFMSong getRandomSong() throws TSDFMQueryException {
        try {
            TSDFMSong song = getRandomItemFromSet(allSongsCache.get(allSongsCacheKey));
            if(song == null) {
                throw new TSDFMQueryException("Found no songs");
            } else {
                return song;
            }
        } catch (ExecutionException ee) {
            log.error("Error getting random song", ee);
            throw new TSDFMQueryException("Error getting random song");
        }
    }

    public TSDFMSong getRandomSongFromAlbum(TSDFMAlbum album) throws TSDFMQueryException {
        TSDFMSong song = getRandomItemFromSet(album.getSongs());
        if(song == null) {
            throw new TSDFMQueryException("Found no songs in album " + album.getName());
        } else {
            return song;
        }
    }

    public TSDFMSong getRandomSongFromArtist(TSDFMArtist artist) throws TSDFMQueryException {
        TSDFMSong song = getRandomItemFromSet(artist.getAllSongs());
        if(song == null) {
            throw new TSDFMQueryException("Found no songs for artist " + artist.getName());
        } else {
            return song;
        }
    }

    <T> T getRandomItemFromSet(Set<T> set) {
        int i = 0;
        int randIdx = random.nextInt(set.size());
        for(T item : set) {
            if(randIdx == i) {
                return item;
            }
            i++;
        }
        return null;
    }

    private static final String TAGS_TABLE = "TSDFM_TAGS";

    private static final String QUERY_EXISTING_TAG =
        "select count(*) from "+TAGS_TABLE+" where path = ? and tag = ?";

    private static final String INSERT_TAG =
        "insert into "+TAGS_TABLE+" (path, tag) values (?, ?)";

    private static final String CREATE_TAGS_TABLE = String.format(
        "create table if not exists %s (path varchar, tag varchar)",
        TAGS_TABLE
    );

    private static final String allSongsCacheKey = "ALLSONGS";
    private final LoadingCache<String, Set<TSDFMSong>> allSongsCache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Set<TSDFMSong>>() {
            @Override
            public Set<TSDFMSong> load(String s) throws Exception {
                return getAllSongs();
            }
        });

}
