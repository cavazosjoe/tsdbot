package org.tsd.tsdbot;

import java.util.Date;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationEntity {

    protected Date date;

    public abstract String getInline();
    public abstract String getPreview();
    public abstract String getFullText();

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
