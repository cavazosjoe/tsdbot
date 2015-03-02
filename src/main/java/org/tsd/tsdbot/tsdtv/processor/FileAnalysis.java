package org.tsd.tsdbot.tsdtv.processor;

import java.io.File;
import java.util.*;

/**
* Created by Joe on 2/28/2015.
*/
public class FileAnalysis {
    private File file;
    private Double bitrate; // kb/s
    private long duration;  // milliseconds

    private TreeMap<Integer, Stream> streamsByInteger = new TreeMap<>();

    // order streams by: video, audio, subtitles
    private TreeMap<StreamType, TreeSet<Stream>> streamsByType = new TreeMap<>(new StreamTypeComparator());

    public FileAnalysis(File file, Double bitrate, long duration, List<Stream> streams) {
        this.file = file;
        this.bitrate = bitrate;
        this.duration = duration;

        for(Stream stream : streams) {
            if(streamsByInteger.containsKey(stream.getStreamNumber()))
                throw new RuntimeException("Detected multiple streams numbered " + stream.getStreamNumber() + " for file " + file.getAbsolutePath());
            streamsByInteger.put(stream.getStreamNumber(), stream);

            if(!streamsByType.containsKey(stream.getStreamType()))
                streamsByType.put(stream.getStreamType(), new TreeSet<>(new Comparator<Stream>() {
                    @Override
                    public int compare(Stream o1, Stream o2) {
                        return Integer.compare(o1.getStreamNumber(), o2.getStreamNumber());
                    }
                }));
            streamsByType.get(stream.getStreamType()).add(stream);
        }
    }

    public File getFile() {
        return file;
    }

    public Double getBitrate() {
        return bitrate;
    }

    public long getDuration() {
        return duration;
    }

    public TreeMap<Integer, Stream> getStreamsByInteger() {
        return streamsByInteger;
    }

    public TreeMap<StreamType, TreeSet<Stream>> getStreamsByType() {
        return streamsByType;
    }

    public int streamCount() {
        return streamsByInteger.size();
    }

    public boolean matchesStructure(FileAnalysis other) {
        if(streamsByInteger.size() == 0 || other.getStreamsByInteger().size() == 0)
            return true;

        if(streamsByInteger.size() != other.getStreamsByInteger().size())
            return false;

        Stream thisStream;
        Stream otherStream;
        for(Integer streamNum : streamsByInteger.keySet()) {
            thisStream = streamsByInteger.get(streamNum);
            otherStream = other.getStreamsByInteger().get(streamNum);

            if(!thisStream.getStreamType().equals(otherStream.getStreamType()))
                return false;

            // compare stream languages if they're not both null
            if (!(thisStream.getLanguage() == null && otherStream.getLanguage() == null)) {
                if (thisStream.getLanguage() == null || otherStream.getLanguage() == null)
                    return false; // one is null but the other isn't
                else if (!thisStream.getLanguage().equals(otherStream.getLanguage()))
                    return false;
            }
        }
        return true;
    }
}
