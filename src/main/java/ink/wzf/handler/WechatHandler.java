package ink.wzf.handler;

import com.blade.mvc.handler.WebSocketHandler;
import com.blade.mvc.websocket.WebSocketContext;
import ink.wzf.service.Wxbot;
import org.apache.http.util.TextUtils;

/**
 * Created by SpereShelde on 2019/1/7.
 */
public class WechatHandler implements WebSocketHandler {

    private Wxbot wxbot;
    Thread thread;
    public WechatHandler(Wxbot wxbot) {
        this.wxbot = wxbot;
        thread = null;
    }

    @Override
    public void onConnect(WebSocketContext webSocketContext) {
        System.out.println("connect");
        webSocketContext.message("https://login.weixin.qq.com/qrcode/" + wxbot.getUuid());
    }

    @Override
    public void onText(WebSocketContext webSocketContext) {
        System.out.println("new");
        if (wxbot.isLogin) {
//            System.out.println("yes");
            webSocketContext.message("Done");
            if (thread != null) {
                System.out.println("not null");
                thread.interrupt();
                if (!TextUtils.isBlank(wxbot.redirectUri)) {// 跳转到登录后页面
                    wxbot.wxNewLoginPage();
                }
                if (!TextUtils.isBlank(wxbot.skey)) {// 初始化微信
                    wxbot.wxInit();
                }
                wxbot.acquireChatList();
                if (wxbot.syncKeyJsonObject != null) {// 开启微信状态通知
                    wxbot.wxStatusNotify();
                }
                thread = new Thread(() -> {
                    System.out.println("new thread");
                    String tip = "1";
                    long currentTimeMillis = System.currentTimeMillis();
                    while (!Thread.interrupted()) {
                        wxbot.syncCheck();
                        if (wxbot.retcode == 0) {
                            if (wxbot.selector == 0) {// 0为正常
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else if (wxbot.selector == 2) {// 新消息
                                System.out.println("select");
                                webSocketContext.message(wxbot.wxSync());
                            }
                        } else if (wxbot.retcode == 1100) {// 1100暂不处理，再请求一次同步
                            System.out.println(1100);
                        }
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        } else {
//            System.out.println("no");
            if (System.currentTimeMillis() - wxbot.time > 60000) {
                wxbot.acquireUuid();
                webSocketContext.message("https://login.weixin.qq.com/qrcode/" + wxbot.getUuid());
                if (thread != null) {
                    thread.interrupt();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                thread = new Thread(() -> {
                    String tip = "1";
                    long currentTimeMillis = System.currentTimeMillis();
                    while (!Thread.interrupted() && !wxbot.checkLogin(tip, currentTimeMillis)) {
//                        System.out.println(wxbot.isLogin);
                        tip = "0";
                        currentTimeMillis++;
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
//                    System.out.println("out - " + wxbot.isLogin);
                });
                thread.start();
            }
        }

        if (webSocketContext.getReqText().equals("kill")) {
            System.out.println("kill");
            webSocketContext.message("kill");
            this.wxbot = new Wxbot();
        }
    }

    @Override
    public void onDisConnect(WebSocketContext webSocketContext) {

    }
}
