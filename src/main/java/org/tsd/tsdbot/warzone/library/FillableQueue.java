package org.tsd.tsdbot.warzone.library;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FillableQueue<T> {

    private List<T> seeds;
    private LinkedList<T> queue = new LinkedList<>();

    public FillableQueue(Collection<T> items) {
        this.seeds = new LinkedList<>(items);
    }

    public FillableQueue() {
        this.seeds = new LinkedList<>();
    }

    public void add(T item) {
        seeds.add(item);
    }

    public T pop() {
        if(queue.isEmpty()) {
            queue.addAll(seeds);
            Collections.shuffle(queue);
        }
        return queue.pop();
    }

}
