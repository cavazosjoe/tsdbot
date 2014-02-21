package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tsd.tsdbot.rss.RssFeedManager;
import org.tsd.tsdbot.rss.RssItem;
import org.tsd.tsdbot.util.HtmlSanitizer;

import javax.naming.OperationNotSupportedException;
import java.net.MalformedURLException;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RssTest {


    @Test
    public void fetchDboRss() throws MalformedURLException, OperationNotSupportedException {

        testFeed("http://destiny.bungie.org/forum/index.php?mode=rss&items=thread_starts");
    }

    @Test
    public void fetchSomeLimeyAtomFeed() throws MalformedURLException, OperationNotSupportedException {

        testFeed("http://www.theregister.co.uk/headlines.atom");
    }

    private void testFeed(String url) throws MalformedURLException, OperationNotSupportedException {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        RssFeedManager man = new RssFeedManager(url, webClient);
        List<RssItem> items = man.sweep();
        assertNotNull(items);
        assertTrue(items.size() > 0);
        RssItem firstItem = items.get(0);
        assertNotNull(firstItem.getInline());
        assertTrue(firstItem.getPreview().length > 0);
        if (firstItem.getFullText() != null) {
            assertTrue(firstItem.getFullText().length > 0);
        }

        // Make sure everything has already been converted to plain text. This may break if we change the implementation.
        if (firstItem.getFullText() != null) {
            assertEquals(firstItem.getFullText()[0], HtmlSanitizer.getText(firstItem.getFullText()[0]));
        }
        assertEquals(firstItem.getPreview()[0], HtmlSanitizer.getText(firstItem.getPreview()[0]));
        assertEquals(firstItem.getInline(), HtmlSanitizer.getText(firstItem.getInline()));
    }



}