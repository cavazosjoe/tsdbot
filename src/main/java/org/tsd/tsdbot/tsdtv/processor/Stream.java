package org.tsd.tsdbot.tsdtv.processor;

/**
* Created by Joe on 2/28/2015.
*/
public class Stream {
    protected StreamType streamType;
    protected int streamNumber;
    protected boolean isDefault;
    protected String rawInfo;
    protected String language;
    protected String codec;

    public Stream(StreamType streamType, int streamNumber, boolean isDefault,
                  String rawInfo, String language, String codec) {
        this.streamType = streamType;
        this.streamNumber = streamNumber;
        this.isDefault = isDefault;
        this.rawInfo = rawInfo;
        this.language = language;
        this.codec = codec;
    }

    public StreamType getStreamType() {
        return streamType;
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getRawInfo() {
        return rawInfo;
    }

    public String getLanguage() {
        return language;
    }

    public String getCodec() {
        return codec;
    }
}
