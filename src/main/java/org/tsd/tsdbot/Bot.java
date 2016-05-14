package org.tsd.tsdbot;

import org.jibble.pircbot.User;

import java.util.LinkedList;

public interface Bot {
    long getBlunderCount();
    void incrementBlunderCnt();

    void sendMessage(String target, String text);
    void onUserList(String channel, User[] users);
    void onPrivateMessage(String sender, String login, String hostname, String message);
    void onAction(String sender, String login, String hostname, String target, String action);
    void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice);
    void onJoin(String channel, String sender, String login, String hostname);
    void onPart(String channel, String sender, String login, String hostname);
    void onNickChange(String oldNick, String login, String hostname, String newNick);
    void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason);
    void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason);
    void onTopic(String channel, String topic, String setBy, long date, boolean changed);
    void onChannelInfo(String channel, int userCount, String topic);
    void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode);
    void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode);
    void onMessage(String channel, String sender, String login, String hostname, String message);
    String[] getChannels();

    void ban(String channel, String hostmask);
    void unBan(String channel, String hostmask);
    void kick(String channel, String nick, String reason);
    void partChannel(String channel, String reason);

    LinkedList<User> getNonBotUsers(String channel);
    boolean addToBlacklist(User user);
    boolean removeFromBlacklist(User user);
    boolean userIsOwner(String nick);
    boolean userHasGlobalPriv(String nick, User.Priv priv);
    boolean userHasPrivInChannel(String nick, String channel, User.Priv priv);
    User getUserFromNick(String channel, String nick);
    void sendMessages(String target, String[] messages);
    void broadcast(String message);
    void shutdownNow();
}
