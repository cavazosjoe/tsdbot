package org.tsd.tsdbot.model.dbo.forum;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tsd.tsdbot.model.BasicEntity;

import java.util.Date;

@DatabaseTable(tableName = "DBO_POST")
public class Post extends BasicEntity {

    @DatabaseField(canBeNull = false, unique = true)
    private int postId;

    @DatabaseField(canBeNull = false)
    private String author;

    @DatabaseField(canBeNull = false)
    private String subject;

    @DatabaseField(canBeNull = false)
    private Date date;

    @DatabaseField(canBeNull = false, dataType = DataType.LONG_STRING)
    private String body;

    public Post() {
    }

    public Post(int id) {
        this.postId = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
