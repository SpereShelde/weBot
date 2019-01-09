package ink.wzf.entities.model;

import io.github.biezhi.anima.Model;

/**
 * Created by SpereShelde on 2018/12/23.
 */
public class Chat extends Model {

    private Long id;
    private String user;
    private int status;
    private long time;

    public Chat() {
    }

    public Chat(Long id, String user, int status, long time) {
        this.id = id;
        this.user = user;
        this.status = status;
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
