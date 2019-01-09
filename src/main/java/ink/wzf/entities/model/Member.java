package ink.wzf.entities.model;

import io.github.biezhi.anima.Model;

/**
 * Created by SpereShelde on 2018/12/24.
 */
public class Member extends Model {

    private String username, name;

    public Member() {
    }

    public Member(String username, String name) {
        this.username = username;
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
