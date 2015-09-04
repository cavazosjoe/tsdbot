package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.NotificationType;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.notifications.NotificationEntity;
import org.tsd.tsdbot.notifications.NotificationManager;

import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
@Function(initialRegex = "^\\.(hbof|hbon|dbof|dbon)\\s+.*")
public class OmniPost extends MainFunctionImpl {

    private Set<NotificationManager> notificationManagers;

    @Inject
    public OmniPost(Bot bot, Set<NotificationManager> notificationManagers) {
        super(bot);
        this.description = "OmniPost notification system. Browse recent posts from the HBO and DBO forums and news feeds";
        this.usage = "USAGE: [ .hbof | .hbon | .dbof | .dbon ] [ list | pv [ postId (optional) ] ]";
        this.notificationManagers = notificationManagers;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        NotificationType type = NotificationType.fromCommand(text);
        if(type == null)
            return;

        NotificationManager<NotificationEntity> mgr = null;
        for(NotificationManager manager : notificationManagers) {
            if(manager.getNotificationType().equals(type)) {
                mgr = manager;
                break;
            }
        }

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {
            bot.sendMessage(channel, usage);
        } else if(cmdParts[1].equals("list")) {
            if(mgr.history() == null || mgr.history().isEmpty())
                bot.sendMessage(channel, "No " + type.getDisplayString() + " posts in recent history");
            for(NotificationEntity notification : mgr.history()) {
                bot.sendMessage(channel,notification.getInline());
            }
        } else if(cmdParts[1].equals("pv")) {
            if(mgr.history().isEmpty()) {
                bot.sendMessage(channel,"No " + type.getDisplayString() +" posts in recent history");
            } else if(cmdParts.length == 2) {
                NotificationEntity mostRecent = mgr.history().getFirst();
                if(mostRecent.isOpened()) bot.sendMessage(channel,"Post " + mostRecent.getKey() + " has already been opened");
                else bot.sendMessage(channel,mostRecent.getPreview());
            } else {
                String postKey = cmdParts[2].trim();
                LinkedList<NotificationEntity> ret = mgr.getNotificationByTail(postKey);
                if(ret.size() == 0) bot.sendMessage(channel,"Could not find " + type.getDisplayString() + " post with ID " + postKey + " in recent history");
                else if(ret.size() > 1) {
                    String returnString = "Found multiple matching " + type.getDisplayString() + " posts in recent history:";
                    for(NotificationEntity not : ret) returnString += (" " + not.getKey());
                    returnString += ". Help me out here";
                    bot.sendMessage(channel,returnString);
                }
                else bot.sendMessage(channel,ret.get(0).getPreview());
            }
        } else {
            bot.sendMessage(channel, usage);
        }
    }

}
