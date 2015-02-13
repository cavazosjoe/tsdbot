package org.tsd.tsdbot;

import com.google.inject.Singleton;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by Joe on 2/6/2015.
 */
@Singleton
public class PrintoutLibrary {

    private static final int MAX_HISTORY = 30;

    private LinkedList<String> printoutIds = new LinkedList<>();

    private HashMap<String, PrintoutFile> printouts = new HashMap<>();

    public void addPrintout(String id, byte[] bytes) {
        printoutIds.addLast(id);
        printouts.put(id, new PrintoutFile(id, bytes));
        if(printoutIds.size() > MAX_HISTORY) {
            String evicted = printoutIds.removeFirst();
            printouts.remove(evicted);
        }
    }

    public byte[] getPrintout(String id) throws FileNotFoundException {
        if(printouts.containsKey(id))
            return printouts.get(id).getBytes();
        else throw new FileNotFoundException();
    }

    class PrintoutFile {
        private String id;
        private byte[] bytes;

        public PrintoutFile(String id, byte[] bytes) {
            this.bytes = bytes;
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
