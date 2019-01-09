package ink.wzf;

import com.blade.Blade;
import ink.wzf.handler.WechatHandler;
import ink.wzf.service.Wxbot;
import io.github.biezhi.anima.Anima;

import static java.lang.Thread.sleep;

/**
 * @author SpereShelde
 * @date 2019/12/15
 */
public class Application {

    public static void main(String[] args) {
        Anima.open("jdbc:sqlite:weBot.db", null, null);
        Wxbot wxbot = new Wxbot();
        Blade.of().listen(7000)
                .webSocket("/wechat", new WechatHandler(wxbot))
                .start(Application.class, args);
    }
}
