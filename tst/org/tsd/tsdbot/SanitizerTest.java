package org.tsd.tsdbot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tsd.tsdbot.util.HtmlSanitizer;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class SanitizerTest {


    @Test
    public void getText() {

        String text = HtmlSanitizer.getText("<img src='hello.jpg'><p>Let me tell you <b>a story</b></p><a><a>");
        assertEquals("Let me tell you a story", text);
    }


}