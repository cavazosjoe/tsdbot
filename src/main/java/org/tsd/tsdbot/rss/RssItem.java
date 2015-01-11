package org.tsd.tsdbot.rss;

import org.tsd.tsdbot.notifications.NotificationEntity;

import java.net.URL;

public class RssItem extends NotificationEntity {
    public static final int DESCRIPTION_LENGTH = 100;
    private String title;
    private String description;
    private String content;
    private URL link;

    @Override
    public String getInline() {
        return title;
    }

    @Override
    public String getPreview() {
//        if (description != null) {
//            return new String[] { description };
//        } else if (content != null) {
//            return new String[] { trimWithEllipsis(content, DESCRIPTION_LENGTH) };
//        }

        return null;
    }

    private String trimWithEllipsis(String content, int maxLength) {
        if (content == null) {
            return null;
        }

        if (content.length() > maxLength) {
            content = content.substring(0, maxLength - 3) + "...";
        }

        return content;
    }

    @Override
    public String[] getFullText() {
        if (content != null) {
            return new String[] {content};
        }

        return null;
    }

    @Override
    public String getKey() {
        return ""; //TODO:
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLink(URL link) {
        this.link = link;
    }

}
