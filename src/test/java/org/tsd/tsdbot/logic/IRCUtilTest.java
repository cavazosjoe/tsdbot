package org.tsd.tsdbot.logic;

import org.junit.Test;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.MiscUtils;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/25/2015.
 */
public class IRCUtilTest {

    private static final String shortString = "ayyy.";

    private static final String longString = "On the other hand, we denounce with righteous indignation and dislike men who are so " +
            "beguiled and demoralized by the charms of pleasure of the moment, so blinded by desire, that they " +
            "cannot foresee the pain and trouble that are bound to ensue; and equal blame belongs to those who " +
            "fail in their duty through weakness of will, which is the same as saying through shrinking from toil " +
            "and pain. These cases are perfectly simple and easy to distinguish. In a free hour, when our power of " +
            "choice is untrammelled and when nothing prevents our being able to do what we like best, every " +
            "pleasure is to be welcomed and every pain avoided. But in certain circumstances and owing to the " +
            "claims of duty or the obligations of business it will frequently occur that pleasures have to be " +
            "repudiated and annoyances accepted. The wise man therefore always holds in these matters to this " +
            "principle of selection: he rejects pleasures to secure other greater pleasures, or else he endures " +
            "pains to avoid worse pains.";

    @Test
    public void testSplitLongString() {

        String[] result = IRCUtil.splitLongString(longString);

        String part1 = "On the other hand, we denounce with righteous indignation and dislike men who are so " +
                "beguiled and demoralized by the charms of pleasure of the moment, so blinded by desire, that they " +
                "cannot foresee the pain and trouble that are bound to ensue; and equal blame belongs to those who " +
                "fail in their duty through weakness of will, which is the same as saying through shrinking from toil " +
                "and pain. These cases are perfectly simple and easy to distinguish. In a free hour, when our power of " +
                "choice is untrammelled and";

        String part2 = " when nothing prevents our being able to do what we like best, every pleasure is to be " +
                "welcomed and every pain avoided. But in certain circumstances and owing to the claims of duty or the " +
                "obligations of business it will frequently occur that pleasures have to be repudiated and annoyances " +
                "accepted. The wise man therefore always holds in these matters to this principle of selection: he " +
                "rejects pleasures to secure other greater pleasures, or else he endures pains to avoid worse pains.";

        assertEquals(2, result.length);
        assertEquals(part1, result[0]);
        assertEquals(part2, result[1]);

        String randomText = MiscUtils.getRandomString(1500);
        String[] randomParts = new String[]{
                randomText.substring(0, 510),
                randomText.substring(510, 1020),
                randomText.substring(1020, 1500)
        };

        String[] randomResult = IRCUtil.splitLongString(randomText);
        assertEquals(randomParts[0], randomResult[0]);
        assertEquals(randomParts[1], randomResult[1]);
        assertEquals(randomParts[2], randomResult[2]);

        assertArrayEquals(new String[]{shortString}, IRCUtil.splitLongString(shortString));

    }

    @Test
    public void testTrimToSingleMsg() {

        String trimmed = "On the other hand, we denounce with righteous indignation and dislike men who are so " +
                "beguiled and demoralized by the charms of pleasure of the moment, so blinded by desire, that they " +
                "cannot foresee the pain and trouble that are bound to ensue; and equal blame belongs to those who " +
                "fail in their duty through weakness of will, which is the same as saying through shrinking from toil " +
                "and pain. These cases are perfectly simple and easy to distinguish. In a free hour, when our power " +
                "of choice is untrammelled ...";

        String result = IRCUtil.trimToSingleMsg(longString);

        assertEquals(trimmed, result);

        assertEquals(shortString, IRCUtil.trimToSingleMsg(shortString));

    }

    @Test
    public void testScrambleNick() {

        String[] normalHandles = new String[]{
                "abc",      "defghijk",
                "aaaaaa",   "au_au_au",
                "frank",    "fronk"
        };

        String[] wontChange = new String[]{
                "bbb",      "bcdfghjk",
                "____",     "_-_-_",
                "AAAA",     "---_"
        };

        for(String handle : normalHandles) {
            assertNotEquals(handle, IRCUtil.scrambleNick(handle));
        }

        for(String handle : wontChange) {
            assertEquals(handle, IRCUtil.scrambleNick(handle));
        }

    }

    @Test
    public void testDetectBot() {

        String[] bots = new String[]{
                "TSDBot",       "Blunderwearbot-PY",
                "BonkBot",      "MyBot",
                "Muh_Bot",      "BotBot",
                "bot",          "A_Really-Bot-like_Bot",
                "TipsFedora",   "kAnBot"
        };

        String[] notBots = new String[]{
                "Schooly_D",    "kanbo",
                "kanbo_t",      "hlmtre",
                "ayyy",         "someBoot",
                "",             "___",
                "bootBootboooooooooooooottttttttt"
        };

        for(String bot : bots) {
            assertTrue(IRCUtil.detectBot(bot));
        }

        for(String notBot : notBots) {
            assertFalse(IRCUtil.detectBot(notBot));
        }

    }
}
