package ink.wzf.controller;

import com.blade.mvc.annotation.GetRoute;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import ink.wzf.service.Wxbot;
import org.apache.http.util.TextUtils;

import static io.github.biezhi.anima.Anima.select;

/**
 * Created by SpereShelde on 2018/12/8.
 */
@Path
public class IndexController {

    @GetRoute("/")
    public String root(){
        return "index.html";
    }

    @GetRoute("index")
    public String index(){
        return "index.html";
    }

    @GetRoute("about")
    public String chat(){
        return "about.html";
    }

    @GetRoute("wechat")
    public String wechat(Request request){
        return "wechat.html";
    }


}
