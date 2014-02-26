package org.tsd.tsdbot;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.xerces.impl.xpath.regex.RegularExpression;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Replacer {

    private static final int BUFFER_SIZE = 20; // 20 message history per channel


    private Map<String, CircularFifoBuffer> channelHistory = new HashMap<>();

    // Detects commands which look like s/find/replace/ username
    private static Pattern commandFormat = Pattern.compile("^s/([^/]+)/([^/]*)(.*)$");

    public void updateHistory(String channel, String message, String sender) {

        CircularFifoBuffer messages;

        if (channelHistory.containsKey(channel)) {
            messages = channelHistory.get(channel);
        } else {
            messages = new CircularFifoBuffer(BUFFER_SIZE);
            channelHistory.put(channel, messages);
        }

        Message m = new Message();
        m.sender = sender;
        m.text = message;

        messages.add(m);

    }

    public String tryGetResponse(String channel, String message) {
        return tryGetResponse(channel, message, null);
    }

    public String tryGetResponse(String channel, String message, String myName) {

        Matcher matcher = commandFormat.matcher(message);
        if (matcher.find()) {
            String find = matcher.group(1);
            String replace = matcher.group(2);
            String theRest = matcher.group(3);

            // Trim off any leading "/g" looking stuff that comes before the username
            String user = theRest.replaceFirst("^(/g ?|/)\\s*", "");

            List<Message> possibilities = getFilteredPossibilities(channel, user);

            for (Message m: possibilities) {
                String replaced = replace(m.text, find, replace);
                if (replaced != null) {

                    return m.sender + " \u0016meant\u000F to say: " + replaced;
                }
            }

            if (myName != null && fuzzyMatches(user, myName)) {
                return "I said what I meant.";
            }

        }

        return null;
    }

    private static String replace(String message, String find, String replace) {

        String modified = message.replace(find, replace);

        if (!modified.equals(message)) {
            return modified;
        }

        return null;
    }

    private List<Message> getFilteredPossibilities(String channel, String targetUser) {
        LinkedList<Message> possibilities = new LinkedList<>();

        if (!channelHistory.containsKey(channel)) {
            return possibilities;
        }

        CircularFifoBuffer messages = channelHistory.get(channel);

        for (Object o: messages) {
            Message m = (Message) o;
            if (targetUser == null || fuzzyMatches(targetUser, m.sender)) {
                possibilities.addFirst(m);
            }
        }

        return possibilities;
    }

    private static boolean fuzzyMatches(String targetUser, String sender) {
        return sender.toLowerCase().startsWith(targetUser.toLowerCase());
    }

    private class Message {
        String text;
        String sender;
    }
}
