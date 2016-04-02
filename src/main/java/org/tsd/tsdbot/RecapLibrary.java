package org.tsd.tsdbot;

import com.google.inject.Singleton;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;

@Singleton
public class RecapLibrary {

    private static final int MAX_HISTORY = 30;

    private LinkedList<String> recapIds = new LinkedList<>();

    private HashMap<String, String> recaps = new HashMap<>();

    public void addRecap(String id, String content) {
        recapIds.addLast(id);
        recaps.put(id, content);
        if(recapIds.size() > MAX_HISTORY) {
            String evicted = recapIds.removeFirst();
            recaps.remove(evicted);
        }
    }

    public String getRecap(String id) throws FileNotFoundException {
        if(recaps.containsKey(id))
            return recaps.get(id);
        else throw new FileNotFoundException();
    }
}
