package org.tsd.tsdbot.functions;

/**
 * Created by Joe on 3/28/2015.
 */
public interface MainFunction {
    String getDescription();
    String getUsage();
    String getListeningRegex();
    void run(String channel, String sender, String ident, String text);
}
