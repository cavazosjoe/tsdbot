package org.tsd.tsdbot.servlets.filename;

public class RandomFilename {
    private final String filename;
    private final byte[] data;

    public RandomFilename(String filename, byte[] data) {
        this.filename = filename;
        this.data = data;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }
}
