package org.tsd.tsdbot;

import org.jibble.pircbot.User;

import java.util.*;

public class TestBot2 extends TSDBot {

    private HashMap<String, Set<User>> channelUsers = new HashMap<>();
    private HashMap<String, LinkedList<String>> linesSent = new HashMap<>();

    @Override
    public synchronized void sendMessage(String target, String text) {
        if(!linesSent.containsKey(target)) {
            linesSent.put(target, new LinkedList<>());
        }
        linesSent.get(target).addFirst(text);
    }

    public void addUser(User user, String... channels) {
        Collection<String> addingChannels = (channels == null) ? channelUsers.keySet() : Arrays.asList(channels);
        for(String c : addingChannels) {
            if(!channelUsers.containsKey(c)) {
                channelUsers.put(c, new HashSet<>());
            }
            channelUsers.get(c).add(user);
        }
    }

    public void removeUser(User user, String... channels) {
        Collection<String> removingChannels = (channels == null) ? channelUsers.keySet() : Arrays.asList(channels);
        for(String c : removingChannels) {
            channelUsers.get(c).add(user);
        }
    }

    public String getLastMessage(String channel) {
        return getLastMessages(channel, 1).get(0);
    }

    public List<String> getAllMessages(String target) {
        return linesSent.get(target);
    }

    public List<String> getLastMessages(String channel, int count) {
        if(count < 1) {
            throw new RuntimeException("number must be larger than 0");
        }
        return linesSent.get(channel).subList(0, count);
    }
}
