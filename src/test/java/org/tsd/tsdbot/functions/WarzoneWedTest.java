package org.tsd.tsdbot.functions;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.jibble.pircbot.User;
import org.jukito.All;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TestBot;
import org.tsd.tsdbot.database.DBConnectionString;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.model.warzone.WarzoneRegular;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Joe on 10/30/2015.
 */
@RunWith(JukitoRunner.class)
public class WarzoneWedTest {

    private static final String channel = "#tsd";

    @Before
    public void initDb(JdbcConnectionSource connectionSource) throws Exception {
        Dao<WarzoneRegular, String> dao = DaoManager.createDao(connectionSource, WarzoneRegular.class);
        dao.deleteBuilder().where().isNotNull("gamertag");
        dao.deleteBuilder().delete();
    }

    @Test
    public void testSingleGame(WarzoneWed warzoneFunction) {
        warzoneFunction.run(channel, "OwnerUser", "schoolyd", ".ww " + testGames[0]);
    }

    @Test
    public void testAddRegulars(JdbcConnectionSource connectionSource,
                                WarzoneWed warzoneFunction,
                                @All(value = "regularsStrings") String rawLine) throws Exception {

        warzoneFunction.addRegulars(connectionSource, rawLine);

        String[][] expected = regularsResultsMap.get(rawLine);
        Dao<WarzoneRegular, String> dao = DaoManager.createDao(connectionSource, WarzoneRegular.class);
        List<WarzoneRegular> regularsInDb = dao.queryForAll();
        assertEquals(expected.length, regularsInDb.size());

        WarzoneRegular checking;
        for(String[] ex : expected) {
            checking = dao.queryForId(ex[0]);
            assertEquals(ex[1], checking.getForumHandle());
        }
    }

    @Test
    public void testProcessGames(JdbcConnectionSource connectionSource,
                                 WarzoneWed warzoneFunction) throws Exception {

        warzoneFunction.processGames(connectionSource, ".ww " + testGames[0]);

    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {

            bindManyNamedInstances(String.class, "regularsStrings",
                    regularsResultsMap.keySet().toArray(new String[regularsResultsMap.size()]));

            bind(String.class).annotatedWith(DBConnectionString.class).toInstance("jdbc:h2:mem:test");
            bind(JdbcConnectionSource.class).toProvider(JdbcConnectionProvider.class);

            TestBot testBot = new TestBot(channel);
            bind(Bot.class).toInstance(testBot);
            testBot.addMainChannelUser(User.Priv.OWNER, "OwnerUser");

            WebClient webClient = new WebClient(BrowserVersion.CHROME);
            webClient.setAjaxController(new AjaxController() {
                @Override
                public boolean processSynchron(HtmlPage page, WebRequest request, boolean async) {
                    return true;
                }
            });
            webClient.getCookieManager().setCookiesEnabled(true);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.setJavaScriptTimeout(10 * 1000);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setRedirectEnabled(true);
            bind(WebClient.class).toInstance(webClient);

            bind(WarzoneWed.class).asEagerSingleton();
            IntegTestUtils.loadFunctions(binder(), WarzoneWed.class);

            requestInjection(testBot);
        }
    }

    private static final String[] testGames = new String[]{
            "https://www.halowaypoint.com/en-us/games/halo-5-guardians/xbox-one/mode/warzone/matches/" +
                    "fe3757f9-67c2-4ded-8768-5821153796c4/players/schooly%20d" +
                    "?gameHistoryMatchIndex=11&gameHistoryGameModeFilter=All"
    };

    private static final HashMap<String, String[][]> regularsResultsMap = new HashMap<>();

    static {
        regularsResultsMap.put(
                ".ww regulars Gamertag One=Handle1, Gamertag Two=Handle 2",
                new String[][]{
                        {"Gamertag One", "Handle1"},
                        {"Gamertag Two", "Handle 2"}
                });
        regularsResultsMap.put(
                ".ww regulars    GamertagOne  =Handle One  ",
                new String[][]{{"GamertagOne", "Handle One"}}
        );
        regularsResultsMap.put(
                ".ww   regulars    GT1=Handle1,GT1=Handle One",
                new String[][]{{"GT1","Handle One"}}
        );
    }
}
