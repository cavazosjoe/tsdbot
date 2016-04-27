package org.tsd.tsdbot.functions;

public interface MainFunction {
    String getDescription();
    String getUsage();
    String getListeningRegex();
    void run(String channel, String sender, String ident, String text);
}
