package org.tsd.tsdbot.tsdfm;

import com.google.inject.name.Names;
import org.apache.commons.lang3.RandomStringUtils;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.DBConnectionString;
import org.tsd.tsdbot.tsdfm.model.TSDFMAlbum;
import org.tsd.tsdbot.tsdfm.model.TSDFMArtist;
import org.tsd.tsdbot.tsdfm.model.TSDFMSong;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JukitoRunner.class)
public class TSDFMLibraryTest {

    @Test
    public void testTagSong(TSDFMLibrary library, DBConnectionProvider connectionProvider) throws Exception {
        TSDFMSong song = library.getRandomSong();
        String songPath = song.getMusicFile().getAbsolutePath();
        library.tagSong(song, Collections.singletonList("test-tag"));

        Connection connection = connectionProvider.get();
        String q = "select * from TSDFM_TAGS";
        try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            result.next();
            assertEquals(songPath, result.getString("path"));
            assertEquals("test-tag", result.getString("tag"));
        }
    }

    @Test
    public void testDuplicateTags(TSDFMLibrary library, DBConnectionProvider connectionProvider) throws Exception {
        TSDFMSong song = library.getRandomSong();
        for(int i=0 ; i < 40 ; i++) {
            library.tagSong(song, Collections.singletonList("test-tag"));
        }

        Connection connection = connectionProvider.get();
        String q = "select count(*) from TSDFM_TAGS";
        try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            result.next();
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    public void testTagAlbum(TSDFMLibrary library, DBConnectionProvider connectionProvider) throws Exception {
        TSDFMSong song = library.getRandomSong();
        TSDFMAlbum album = song.getAlbum();
        String tag = RandomStringUtils.randomAlphabetic(10);
        library.tagAlbum(album, Collections.singletonList(tag));

        Connection connection = connectionProvider.get();
        String q = "select count(*) from TSDFM_TAGS";
        try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            result.next();
            assertEquals(album.getSongs().size(), result.getInt(1));
        }
    }

    @Test
    public void testTagArtist(TSDFMLibrary library, DBConnectionProvider connectionProvider) throws Exception {
        TSDFMSong song = library.getRandomSong();
        TSDFMArtist artist = song.getArtist();
        String tag1 = RandomStringUtils.randomAlphabetic(10);
        String tag2 = RandomStringUtils.randomAlphabetic(10);
        library.tagArtist(artist, Arrays.asList(tag1, tag2));

        Connection connection = connectionProvider.get();
        String q = "select count(*) from TSDFM_TAGS";
        try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            result.next();
            assertEquals(artist.getAllSongs().size()*2, result.getInt(1));
        }
    }

    @Test
    public void testQuerySong(TSDFMLibrary library) throws Exception {
        TSDFMSong song = library.getSong("tommy", null, null);
        assertNotNull(song);
    }

    @Test
    public void testGetSongFromPath(TSDFMLibrary library) throws Exception {
        TSDFMSong song = library.getSongFromPath("C:\\Users\\Joe\\TSDFM\\Randy Rogers Band\\Like it Used to Be" +
                "\\Randy Rogers Band - Like It Used To Be - 01 - Disappear.mp3");
        assertNotNull(song);
    }

    @Test
    public void testGetSongsForTags(TSDFMLibrary library) throws Exception {
        TSDFMSong song = library.getRandomSong();
        TSDFMArtist artist = song.getArtist();
        library.tagArtist(artist, Collections.singletonList("artist-tag"));

        Set<TSDFMSong> songs = library.getAllSongsForTags(Arrays.asList("artist-tag", "album-tag", "tip-top"));
        assertEquals(artist.getAllSongs().size(), songs.size());
    }

    @Before
    public void initDb(TSDFMLibrary library, DBConnectionProvider connectionProvider) throws Exception {
        library.initDB();
        Connection connection = connectionProvider.get();
        String q = "delete from TSDFM_TAGS";
        try(PreparedStatement ps = connection.prepareStatement(q)) {
            ps.execute();
        }
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            bind(Random.class).toInstance(new Random());

            bind(File.class).annotatedWith(Names.named("tsdfmLibrary"))
                    .toInstance(new File(IntegTestUtils.loadProperty("tsdfmLib")));

            bind(String.class).annotatedWith(DBConnectionString.class)
                    .toInstance("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

            bind(DBConnectionProvider.class);
            bind(TSDFMLibrary.class);
        }
    }
}
