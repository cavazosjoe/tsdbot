package org.tsd.tsdbot.scheduled;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobExecutionException;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.markov.MarkovFileManager;

import java.util.Random;

@RunWith(MockitoJUnitRunner.class)
public class DboForumSweeperJobTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DboForumSweeperJob dboForumSweeperJob;
    private MarkovFileManager markovFileManager;

    @Before
    public void setup() {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getCookieManager().setCookiesEnabled(false);

        dboForumSweeperJob = new DboForumSweeperJob(
                webClient,
                new JdbcConnectionProvider("jdbc:h2:mem:testdb"),
                new MarkovFileManager(temporaryFolder.getRoot(), new Random())
        );
    }

    @Test
    public void testSweep() throws JobExecutionException {
        dboForumSweeperJob.execute(null);
    }

}
