package org.tsd.tsdbot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tsd.tsdbot.database.TSDDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class TomCruiseTest {

    String insertQuote = "insert into TomCruiseQuotes ( id, quote ) values ( 9999, 'Get with it. Millions of galaxies of hundreds of millions of stars, in a speck on one in a blink. That''s us, lost in space. The cop, you, me… Who notices?' )";
    String deleteQuote = "delete from TomCruiseQuotes where id = 9999";

    String insertClip = "insert into TomCruiseClips ( id, clip ) values ( 9999, 'https://www.youtube.com/watch?v=7yP9MmzyTIg' )";
    String deleteClip = "delete from TomCruiseClips where id = 9999";

    @Before
    public void setup() {
        TSDDatabase.initialize();


        try (Connection conn = TSDDatabase.getConnection()) {

            try (PreparedStatement ps = conn.prepareCall(insertQuote)) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareCall(insertClip)) {
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @After
    public void tearDown() {
        try (Connection conn = TSDDatabase.getConnection()) {

            try (PreparedStatement ps = conn.prepareCall(deleteQuote)) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareCall(deleteClip)) {
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testRandomClip() throws ClassNotFoundException, SQLException {

        assertNotNull(TomCruise.getRandomClip());

    }

    @Test
    public void testRandomQuote() throws ClassNotFoundException, SQLException {

        assertNotNull(TomCruise.getRandomQuote());

    }

    @Test
    public void testRandomWhatever() throws ClassNotFoundException, SQLException {

        assertNotNull(TomCruise.getRandom());

    }


}
