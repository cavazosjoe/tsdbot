package org.tsd.tsdbot.util;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import java.util.Iterator;

/**
 * Created by Joe on 1/7/2015.
 */
public class CircularBuffer<T> implements Iterable<T> {

    private CircularFifoBuffer buffer;

    public CircularBuffer(int size) {
        this.buffer = new CircularFifoBuffer(size);
    }

    public void add(T item) {
        buffer.add(item);
    }

    public int size() {
        return buffer.size();
    }

    @Override
    public Iterator<T> iterator() {
        return buffer.iterator();
    }
}
