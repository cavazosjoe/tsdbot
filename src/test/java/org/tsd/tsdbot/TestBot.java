package org.tsd.tsdbot;

import com.google.inject.Singleton;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 3/27/2015.
 */
public class TestBot implements Bot {

    private HashMap<String, LinkedList<String>> linesSent = new HashMap<>();

    @Override
    public long getBlunderCount() {
        return 0;
    }

    @Override
    public void incrementBlunderCnt() {}

    @Override
    public void sendMessage(String target, String text) {
        if(!linesSent.containsKey(target))
            linesSent.put(target, new LinkedList<String>());
        linesSent.get(target).addFirst(text);
    }

    @Override public void onUserList(String channel, User[] users) {}
    @Override public void onPrivateMessage(String sender, String login, String hostname, String message) {}
    @Override public void onAction(String sender, String login, String hostname, String target, String action) {}
    @Override public void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {}
    @Override public void onJoin(String channel, String sender, String login, String hostname) {}
    @Override public void onPart(String channel, String sender, String login, String hostname) {}
    @Override public void onNickChange(String oldNick, String login, String hostname, String newNick) {}
    @Override public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {}
    @Override public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {}
    @Override public void onTopic(String channel, String topic, String setBy, long date, boolean changed) {}
    @Override public void onChannelInfo(String channel, int userCount, String topic) {}
    @Override public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {}
    @Override public void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {}
    @Override public void onMessage(String channel, String sender, String login, String hostname, String message) {}

    @Override public void ban(String channel, String hostmask) {}
    @Override public void unBan(String channel, String hostmask) {}
    @Override public void kick(String channel, String nick, String reason) {}
    @Override public void partChannel(String channel, String reason) {}

    @Override
    public LinkedList<User> getNonBotUsers(String channel) {
        return null;
    }

    @Override
    public boolean userHasGlobalPriv(String nick, User.Priv priv) {
        return false;
    }

    @Override
    public boolean userHasPrivInChannel(String nick, String channel, User.Priv priv) {
        return false;
    }

    @Override
    public User getUserFromNick(String channel, String nick) {
        return null;
    }

    @Override
    public void sendMessages(String target, String[] messages) {

    }

    @Override
    public void broadcast(String message) {

    }

    public String getLastMessage(String target) {
        return getLastMessages(target, 1).get(0);
    }

    public List<String> getLastMessages(String target, int number) {
        if(number < 1)
            throw new RuntimeException("number must be larger than 0");
        return linesSent.get(target).subList(0, number);
    }

    public List<String> getAllMessages(String target) {
        return linesSent.get(target);
    }
}
