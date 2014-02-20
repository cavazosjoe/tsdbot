package org.tsd.tsdbot;

import org.h2.store.fs.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class DatabaseTest {

    @Before
    public void setup() throws ClassNotFoundException {

        Class.forName("org.h2.Driver"); // if this fails, make sure lib/h2-1.3.175.jar is in your class path

        getOutputDir().toFile().mkdirs();
    }

    @After
    public void cleanup() throws Exception {

        FileUtils.deleteRecursive(getOutputDir().toString(), false);
    }

    @Test
    public void autoCreateDatabase() throws ClassNotFoundException, SQLException {

        Connection conn = DriverManager.getConnection(getConnectionString("empty-test-db"));
        conn.close();

    }

    @Test
    public void insertIntoTable() throws SQLException {

        try (Connection conn = DriverManager.getConnection(getConnectionString("insert-test-db"))) {

            try (PreparedStatement ps = conn.prepareStatement("create table cruise ( id int, quote text);")) {
                ps.executeUpdate();
            }

            String cruiseQuote = "you've lost that loving feeling";

            try (PreparedStatement ps = conn.prepareStatement("insert into cruise (id, quote) values (1, ?);")) {
                ps.setString(1, cruiseQuote);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("select quote from cruise where id = 1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(cruiseQuote, rs.getString(1));
                }
            }

        }
    }

    private Path getOutputDir() {
        return Paths.get(System.getProperty("java.io.tmpdir")).resolve("tsdbot-db-test");
    }

    private String getConnectionString(String dbName) {
        String dbLocation = getOutputDir().resolve(dbName).toString();
        return "jdbc:h2:" + dbLocation;
    }

}
