package org.tsd.tsdbot;

import java.util.Date;
import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationEntity {

    protected Date date;
    protected boolean opened = false;

    public abstract String getInline();
    public abstract String[] getPreview();
    public abstract String[] getFullText();
    public abstract String getKey();

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
