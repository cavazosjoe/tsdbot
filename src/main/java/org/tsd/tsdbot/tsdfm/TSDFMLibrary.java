package org.tsd.tsdbot.tsdfm;

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
import java.util.stream.Collectors;

@Singleton
public class TSDFMLibrary implements Persistable {

    private static final Logger log = LoggerFactory.getLogger(TSDFMLibrary.class);

    private final DBConnectionProvider connectionProvider;
    private final File libraryDirectory;
    private final TreeSet<TSDFMArtist> artists = new TreeSet<>();

    @Inject
    public TSDFMLibrary(DBConnectionProvider connectionProvider,
                        @Named("tsdtvLibrary") File libraryDirectory) {
        this.libraryDirectory = libraryDirectory;
        this.connectionProvider = connectionProvider;
        load();
    }

    @Override
    public void initDB() throws SQLException {
        log.info("Initializing TSDTV database");
        Connection connection = connectionProvider.get();

        // create song tag table if it doesn't exist
        String tagsTable = "TSDFM_TAGS";
        String createTagsTable = String.format(
                "create table if not exists %s " +
                "(" +
                    "path varchar," +
                    "tag varchar," +
                    "constraint unique(path, tag)" +
                ")", tagsTable);
        try(PreparedStatement ps = connection.prepareStatement(createTagsTable)) {
            log.info("TSDFM_TAGS: {}", createTagsTable);
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
    }

    String sanitizeTag(String tag) {
        return tag.trim();
    }

    public void tagSong(TSDFMSong song, String... tags) throws SQLException {
        String queryTagFormat = "select count(*) from TSDFM_TAGS where path = '%s' and tag = '%s'";
        String insertTagFormat = "insert into TSDFM_TAGS (path, tag) values ('%s', '%s')";

        String songPath = song.getMusicFile().getAbsolutePath();
        String countQuery;
        String insertStatement;
        Connection connection = connectionProvider.get();
        for(String tag : tags) {
            tag = sanitizeTag(tag);
            countQuery = String.format(queryTagFormat, songPath, tag);
            try(PreparedStatement ps = connection.prepareStatement(countQuery) ; ResultSet result = ps.executeQuery()) {
                result.next();
                if(result.getInt(1) == 0) {
                    log.info("Could not find tag '{}' for song {}, adding...", tag, songPath);
                    insertStatement = String.format(
                            insertTagFormat,
                            songPath,
                            tag);
                    try(PreparedStatement ps1 = connection.prepareCall(insertStatement)) {
                        ps1.executeUpdate();
                    }
                } else {
                    log.warn("Tag {} already exists for file {} -- skipping...", tag, songPath);
                }
            }
        }
    }

    public void tagAlbum(TSDFMAlbum album, String... tags) throws SQLException {
        for(TSDFMSong song : album.getSongs()) {
            tagSong(song, tags);
        }
    }

    public void tagArtist(TSDFMArtist artist, String... tags) throws SQLException {
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

    public Set<TSDFMSong> getAllSongsForTags(String... tags) throws SQLException {
        Connection dbConn = connectionProvider.get();

        StringBuilder innerQueryBuilder = new StringBuilder();
        for(String tag : tags) {
            if(innerQueryBuilder.length() > 0) {
                innerQueryBuilder.append(",");
            } else {
                innerQueryBuilder.append("select ");
            }
            innerQueryBuilder.append("'").append(tag).append("'");
        }

        String query = "select path from TSDFM_TAGS where tag in (" + innerQueryBuilder.toString() + ")";
        Set<String> matchingSongs = new HashSet<>();
        try(PreparedStatement ps = dbConn.prepareStatement(query) ; ResultSet result = ps.executeQuery()) {
            while(result.next()) {
                matchingSongs.add(result.getString(1));
            }
        }


    }

}
