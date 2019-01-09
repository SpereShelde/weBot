package ink.wzf.entities.model;

import io.github.biezhi.anima.Model;

/**
 * Created by SpereShelde on 2018/12/20.
 */
public class Message extends Model {

    private long id, time;
    private int type;
    private String userFrom, userTo, content;

    public Message() {
    }

    public Message(long id, long time, int type, String userFrom, String userTo, String content) {
        this.id = id;
        this.time = time;
        this.type = type;
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getUserFrom() {
        return userFrom;
    }

    public void setUserFrom(String userFrom) {
        this.userFrom = userFrom;
    }

    public String getUserTo() {
        return userTo;
    }

    public void setUserTo(String userTo) {
        this.userTo = userTo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
