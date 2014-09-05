package org.tsd.tsdbot.util;

import org.apache.commons.lang3.StringUtils;
import org.tsd.tsdbot.functions.Archivist;

/**
 * Created by Joe on 7/7/2014.
 */
public class ArchivistUtil {

    public static String getRawJoin(long timestamp, String nick, String ident, String host, String channel) {
        return concatParts(Archivist.EventType.JOIN.toString(), Long.toString(timestamp), nick, ident, host, channel);
    }

    public static String getRawPart(long timestamp, String nick, String ident, String channel) {
        return concatParts(Archivist.EventType.PART.toString(), Long.toString(timestamp), nick, ident, channel);
    }

    public static String getRawQuit(long timestamp, String nick, String ident, String reason) {
        return concatParts(Archivist.EventType.QUIT.toString(), Long.toString(timestamp), nick, ident, reason);
    }

    public static String getRawMessage(long timestamp, String nick, String ident, String message) {
        return concatParts(Archivist.EventType.MESSAGE.toString(), Long.toString(timestamp), nick, ident, message);
    }

    public static String getRawChannelMode(long timestamp, String nick, String mode, String channel) {
        return concatParts(Archivist.EventType.CHANNEL_MODE.toString(), Long.toString(timestamp), nick, mode, channel);
    }

    public static String getRawUserMode(long timestamp, String nick, String mode, String user) {
        return concatParts(Archivist.EventType.USER_MODE.toString(), Long.toString(timestamp), nick, mode, user);
    }

    public static String getRawTopicChange(long timestamp, String nick, String topic) {
        return concatParts(Archivist.EventType.TOPIC.toString(), Long.toString(timestamp), nick, topic);
    }

    public static String getRawAction(long timestamp, String nick, String ident, String action) {
        return concatParts(Archivist.EventType.ACTION.toString(), Long.toString(timestamp), nick, ident, action);
    }

    public static String getRawNickChange(long timestamp, String oldnick, String newnick) {
        return concatParts(Archivist.EventType.NICK_CHANGE.toString(), Long.toString(timestamp), oldnick, newnick);
    }

    public static String getRawKick(long timestamp, String kicker, String kickee, String channel, String reason) {
        return concatParts(Archivist.EventType.KICK.toString(), Long.toString(timestamp), kicker, kickee, channel, reason);
    }

    public static String compileMessage(String[] parts, int idx) {
        StringBuilder msg = new StringBuilder();
        boolean first = true;
        for(int i=idx ; i < parts.length ; i++) {
            if(!first) msg.append(" ");
            msg.append(parts[i]);
            first = false;
        }
        return msg.toString();
    }

    private static String concatParts(String... args) {
        return StringUtils.join(args, " ");
    }
}
