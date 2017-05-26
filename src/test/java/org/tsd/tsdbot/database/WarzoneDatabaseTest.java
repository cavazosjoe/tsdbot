package org.tsd.tsdbot.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.model.warzone.*;
import org.tsd.tsdbot.model.warzone.dao.WarzoneGameDao;
import org.tsd.tsdbot.model.warzone.dao.WarzoneGamePlayerDao;
import org.tsd.tsdbot.model.warzone.dao.WarzoneNightDao;

import java.util.Date;
import java.util.Random;

import static org.junit.Assert.*;

@RunWith(JukitoRunner.class)
@Ignore
public class WarzoneDatabaseTest {

    @Test
    public void testModel(JdbcConnectionSource jdbcConnectionSource) {
        try {

            Date d = new Date();

            WarzoneNight night = new WarzoneNight();
            night.setDate(d);

            WarzoneNightDao nightDao = new WarzoneNightDao(jdbcConnectionSource);
            nightDao.create(night);

            night = nightDao.queryForId(night.getId());
            assertEquals(d, night.getDate());

            WarzoneGame game = new WarzoneGame();
            game.setId(new Random().nextInt(1000));
            game.setNight(night);
            game.setGametype("Raid");
            game.setMap("Apex");
            game.setDuration(600); //10 minutes
            game.setTeamColor(TeamColor.red);
            game.setTeamScore(1000);
            game.setEnemyScore(500);
            game.setWin(true);

            WarzoneGameDao gameDao = new WarzoneGameDao(jdbcConnectionSource);
            gameDao.create(game);

            assertEquals(1, night.getGames().size());
            assertTrue(night.getGames().contains(game));

            WarzoneGamePlayer player1 = new WarzoneGamePlayer();
            player1.setGame(game);
            player1.setGamertag("Schooly_1");
            player1.setTeamColor(TeamColor.red);
            player1.setBasesCaptured(1);
            player1.setBossTakedowns(2);
            player1.setSpartanKills(100);
            player1.setNpcKills(3);
            player1.setKda(1000);
            player1.setPowerWeaponKills(4);

            WarzoneGamePlayerDao playerDao = new WarzoneGamePlayerDao(jdbcConnectionSource);
            playerDao.create(player1);
            gameDao.refresh(game);

            assertEquals(1, game.getPlayers().size());
            assertTrue(game.getPlayers().contains(player1));

            WarzoneGamePlayer player2 = new WarzoneGamePlayer();
            player2.setGame(game);
            player2.setGamertag("Schooly_2");
            player2.setTeamColor(TeamColor.blue);

            playerDao.create(player2);
            gameDao.refresh(game);

            assertEquals(2, game.getPlayers().size());
            assertTrue(game.getPlayers().contains(player2));

            WarzoneRegular regular = new WarzoneRegular();
            regular.setGamertag("Schooly_1");
            regular.setForumHandle("Schooly 1");

            Dao<WarzoneRegular, String> regularDao = DaoManager.createDao(jdbcConnectionSource, WarzoneRegular.class);
            regularDao.create(regular);

            nightDao.delete(night);

            assertEquals(0, playerDao.queryForAll().size());
            assertEquals(0, gameDao.queryForAll().size());
            assertEquals(0, nightDao.queryForAll().size());
            assertEquals(1, regularDao.queryForAll().size());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            if(jdbcConnectionSource.isOpen()) {
                jdbcConnectionSource.closeQuietly();
            }
        }
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            bind(String.class).annotatedWith(DBConnectionString.class).toInstance("jdbc:h2:mem:test");
            bind(JdbcConnectionSource.class).toProvider(JdbcConnectionProvider.class);
        }
    }
}
