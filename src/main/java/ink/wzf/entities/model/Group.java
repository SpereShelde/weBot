package ink.wzf.entities.model;

import io.github.biezhi.anima.Model;

/**
 * Created by SpereShelde on 2018/12/23.
 */
public class Group extends Model {

    private long id;
    private String name, me;

    public Group() {
    }

    public Group(long id, String name, String me) {
        this.id = id;
        this.name = name;
        this.me = me;
    }

    public String getMe() {
        return me;
    }

    public void setMe(String me) {
        this.me = me;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
