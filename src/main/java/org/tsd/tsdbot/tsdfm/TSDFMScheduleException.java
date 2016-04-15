package org.tsd.tsdbot.tsdfm;

public class TSDFMScheduleException extends Exception {
    public TSDFMScheduleException(String format, String... args) {
        super(String.format(format, args));
    }
}
