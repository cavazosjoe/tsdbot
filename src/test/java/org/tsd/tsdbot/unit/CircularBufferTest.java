package org.tsd.tsdbot.unit;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.tsd.tsdbot.util.CircularBuffer;

import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/26/2015.
 */
public class CircularBufferTest {

    @Test
    public void testCircularBuffer() {

        int bufSize = 10;
        CircularBuffer<String> buffer = new CircularBuffer<>(bufSize);

        assertEquals(0, buffer.size());

        String string1 = RandomStringUtils.randomAlphanumeric(10);
        buffer.add(string1);

        assertEquals(1, buffer.size());
        for(String s : buffer)
            assertEquals(string1, s);

        // add {bufSize} items to buffer, which should push out string1
        HashSet<String> addedStrings = new HashSet<>();
        for(int i=0 ; i < bufSize ; i++) {
            addToBufferAndSet(buffer, addedStrings, RandomStringUtils.randomAlphanumeric(10));
        }

        assertEquals(bufSize, buffer.size());

        for(String s : buffer) {
            assertTrue(addedStrings.contains(s));
            assertNotEquals(string1, s); // make sure string1 got pushed out
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeZeroBuffer() {
        CircularBuffer buffer = new CircularBuffer(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSizeBuffer() {
        CircularBuffer buffer = new CircularBuffer(-1);
    }

    private <T> void addToBufferAndSet(CircularBuffer<T> buffer, HashSet<T> set, T item) {
        buffer.add(item);
        set.add(item);
    }
}
