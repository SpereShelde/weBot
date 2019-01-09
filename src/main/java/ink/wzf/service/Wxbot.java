package ink.wzf.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ink.wzf.entities.model.Chat;
import ink.wzf.entities.model.Group;
import ink.wzf.entities.model.Member;
import ink.wzf.entities.model.Message;
import io.github.biezhi.anima.Anima;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.TextUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.biezhi.anima.Anima.me;
import static io.github.biezhi.anima.Anima.select;

/**
 * Created by SpereShelde on 2018/12/19.
 */
public class Wxbot {

    private CloseableHttpClient httpClient;
    private String uuid;
    public String redirectUri;
    private String baseUrl;
    public String skey;
    private String wxsid;
    private String wxuin;
    private String passTicket;
    private String deviceId;
    private String userName;
    public int retcode;
    public int selector;
    private boolean away;
    private long awayTime;
    public boolean isLogin;
    public long time;

    private JsonParser jsonParser = new JsonParser();
    public JsonObject syncKeyJsonObject;

    private List<String> specialUsers = Arrays.asList("newsapp", "fmessage", "filehelper", "weibo", "qqmail",
            "fmessage", "tmessage", "qmessage", "qqsync", "floatbottle", "lbsapp", "shakeapp", "medianote", "qqfriend",
            "readerapp", "blogapp", "facebookapp", "masssendapp", "meishiapp", "feedsapp", "voip", "blogappweixin",
            "weixin", "brandsessionholder", "weixinreminder", "wxid_novlwrv3lqwv11", "gh_22b87fa7cb3c",
            "officialaccounts", "notification_messages", "wxid_novlwrv3lqwv11", "gh_22b87fa7cb3c", "wxitil",
            "userexperience_alarm", "notification_messages");

    public String getUuid() {
        return uuid;
    }

    public String getSkey() {
        return skey;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public JsonObject getSyncKeyJsonObject() {
        return syncKeyJsonObject;
    }

    public Wxbot() {
        System.setProperty("jsse.enableSNIExtension", "false");
        System.setProperty("https.protocols", "TLSv1");
        isLogin = false;
        away = false;
        time = 0L;
        awayTime = System.currentTimeMillis() / 1000;
        try {
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
                    new String[] { "TLSv1" }, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.httpClient = httpClient;
    }

    public static void main(String[] args) {
        Anima.open("jdbc:sqlite:weBot.db", null, null);

        Wxbot wxbot = new Wxbot();
        wxbot.acquireUuid();
        if (!TextUtils.isBlank(wxbot.uuid)) {
            wxbot.downQrCode();// 下载二维码图片
        }
        wxbot.login();// 登录操作
        if (!TextUtils.isBlank(wxbot.redirectUri)) {// 跳转到登录后页面
            wxbot.wxNewLoginPage();
        }
        if (!TextUtils.isBlank(wxbot.skey)) {// 初始化微信
            wxbot.wxInit();
        }
        wxbot.acquireChatList();
        if (wxbot.syncKeyJsonObject != null) {// 开启微信状态通知
            wxbot.wxStatusNotify();
            wxbot.listenMsg();
        }
    }

    public void acquireUuid() {
        String url = "https://login.weixin.qq.com/jslogin";
        CloseableHttpResponse response = null;
        try {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("appid", "wx782c26e4c19acffb"));
            list.add(new BasicNameValuePair("fun", "new"));
            list.add(new BasicNameValuePair("lang", "zh_CN"));
            list.add(new BasicNameValuePair("_", Long.toString(System.currentTimeMillis())));

            URIBuilder urlBuilder = new URIBuilder();
            urlBuilder.setPath(url);
            urlBuilder.setParameters(list);
            URI uri = urlBuilder.build();

            HttpGet httpGet = new HttpGet(uri);
            System.out.println("获取uuid");
//            System.out.println(httpGet.getRequestLine());
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
//                System.out.println(responseContent);
                if (!TextUtils.isBlank(responseContent)) {
                    String code = this.findStr("window.QRLogin.code = (\\d+);", responseContent);
                    if (!TextUtils.isEmpty(code) && code.equals("200")) {
                        this.uuid = this.findStr("window.QRLogin.uuid = \"(.*)\";", responseContent);
                        this.time = System.currentTimeMillis();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void downQrCode() {
        String url = "https://login.weixin.qq.com/qrcode/" + this.uuid;
        System.out.println("请扫码登录：" + url);
    }

    private void downImg(Long MsgID, int type) {
        String url = "";
        switch (type) {
             default:
            case 3: url = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxgetmsgimg"; break;
            case 43:
            case 62: url = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxgetvideo"; break;
            case 34: url = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxgetvoice"; break;
        }
        CloseableHttpResponse response = null;
        try {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("MsgID", MsgID + ""));
            list.add(new BasicNameValuePair("skey", skey));

            URIBuilder urlBuilder = new URIBuilder();
            urlBuilder.setPath(url);
            urlBuilder.setParameters(list);
            URI uri = urlBuilder.build();

            HttpGet httpGet = new HttpGet(uri);
            System.out.println("下载图片");
//            System.out.println(httpGet.getRequestLine());
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                FileOutputStream fos = new FileOutputStream(new File( MsgID + ".jpeg"));
                int l = -1;
                byte[] b = new byte[1024];
                while ((l = is.read(b)) != -1) {
                    fos.write(b, 0, l);
                }
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public void login() {
        String tip = "1";// 首次时为1
        long currentTimeMillis = System.currentTimeMillis();// 首次时为系统时间，之后每次请求加1
        while (!this.checkLogin(tip, currentTimeMillis)) {
            tip = "0";
            currentTimeMillis++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!TextUtils.isBlank(this.redirectUri)) {// 跳转到登录后页面
            this.wxNewLoginPage();
        }
        if (!TextUtils.isBlank(this.skey)) {// 初始化微信
            this.wxInit();
        }
        this.acquireChatList();
        if (this.syncKeyJsonObject != null) {// 开启微信状态通知
            this.wxStatusNotify();
            this.listenMsg();
        }
    }

    public boolean checkLogin(String tip, long currentTimeMillis) {
        String url = "https://login.wx.qq.com/cgi-bin/mmwebwx-bin/login";
        CloseableHttpResponse response = null;
        try {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("loginicon", "true"));
            list.add(new BasicNameValuePair("uuid", this.uuid));
            list.add(new BasicNameValuePair("tip", tip));
            list.add(new BasicNameValuePair("r", this.r()));
            list.add(new BasicNameValuePair("_", Long.toString(currentTimeMillis)));

            URIBuilder urlBuilder = new URIBuilder();
            urlBuilder.setPath(url);
            urlBuilder.setParameters(list);
            URI uri = urlBuilder.build();

            HttpGet httpGet = new HttpGet(uri);
            System.out.println("检测登录");
//            System.out.println(httpGet.getRequestLine());
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
//                System.out.println(responseContent);
                if (!TextUtils.isBlank(responseContent)) {
                    String code = this.findStr("window.code=(\\d+);", responseContent);
                    if (TextUtils.isEmpty(code)) {

                    } else if (code.equals("200")) {// 点击登录后执行
                        redirectUri = this.findStr("window.redirect_uri=\"(\\S+?)\";", responseContent);
                        baseUrl = redirectUri.substring(0, redirectUri.lastIndexOf("/"));
                        System.out.println("登录成功");
                        this.isLogin = true;
                        return true;
                    } else if (code.equals("201")) {// 扫描成功，还未点击登录
                        System.out.println("扫描成功，请点击登录");
                    } else if (code.equals("408")) {// 二维码未扫描，登录超时

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void wxNewLoginPage() {
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(redirectUri + "&fun=new&version=v2");
            System.out.println("跳转到新的登录页面");
//            System.out.println(httpGet.getRequestLine());
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
                skey = this.findStr("<skey>(\\S+)</skey>", responseContent);
                wxsid = this.findStr("<wxsid>(\\S+)</wxsid>", responseContent);
                wxuin = this.findStr("<wxuin>(\\S+)</wxuin>", responseContent);
                passTicket = this.findStr("<pass_ticket>(\\S+)</pass_ticket>", responseContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void wxInit() {
        String url = baseUrl + "/webwxinit";
        CloseableHttpResponse response = null;
        try {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("r", ""+System.currentTimeMillis()));
            list.add(new BasicNameValuePair("lang", "zh_CN"));
            list.add(new BasicNameValuePair("pass_ticket", passTicket));

            URIBuilder urlBuilder = new URIBuilder();
            urlBuilder.setPath(url);
            urlBuilder.setParameters(list);
            URI uri = urlBuilder.build();

            deviceId = "e" + System.currentTimeMillis() + wxuin.substring(0, 2);// 生成15位随机数
            String json = "{\"BaseRequest\":{\"Uin\":\"" + wxuin + "\",\"Sid\":\"" + wxsid + "\"," + "\"Skey\":\""
                    + skey + "\",\"DeviceID\":\"" + deviceId + "\"}}";
            StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(stringEntity);
            System.out.println("微信初始化");
//            System.out.println(httpPost.getRequestLine());
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
//                System.out.println(responseContent);
                JsonObject jsonObject = jsonParser.parse(responseContent).getAsJsonObject();
                syncKeyJsonObject = jsonObject.getAsJsonObject("SyncKey");
//                System.out.println(syncKeyJsonObject);
                userName = jsonObject.getAsJsonObject("User").get("UserName").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void wxStatusNotify() {
        String url = baseUrl + "/webwxstatusnotify";
        CloseableHttpResponse response = null;
        try {
            String json = "{\"BaseRequest\":{\"Uin\":\"" + wxuin + "\",\"Sid\":\"" + wxsid + "\"," + "\"Skey\":\""
                    + skey + "\",\"DeviceID\":\"" + deviceId + "\"},\"Code\":3," + "\"FromUserName\":" + userName
                    + ",\"ToUserName\":" + userName + ",\"ClientMsgId\":"
                    + System.currentTimeMillis() + "}";
            StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(stringEntity);
            System.out.println("开始微信状态通知");
//            System.out.println(httpPost.getRequestLine());
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
                JsonObject jsonObject = jsonParser.parse(responseContent).getAsJsonObject();
                int ret = jsonObject.getAsJsonObject("BaseResponse").get("Ret").getAsInt();
                if(ret == 0){
                    System.out.println("开始微信状态通知: 成功");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void acquireChatList() {
        String url = baseUrl + "/webwxgetcontact?pass_ticket=" + passTicket + "&skey=" + skey + "&r=" + this.r();
        CloseableHttpResponse response = null;
        try {
            StringEntity stringEntity = new StringEntity("{}", ContentType.APPLICATION_JSON);
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(stringEntity);
            System.out.println("获取联系人列表");
//            System.out.println(httpPost.getRequestLine());
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
                JsonObject jsonObject = jsonParser.parse(responseContent).getAsJsonObject();
                JsonArray jsonArray = jsonObject.getAsJsonArray("MemberList");
                jsonArray.forEach(jsonElement -> {
                    JsonObject jo = jsonElement.getAsJsonObject();
                    String un = jo.get("UserName").getAsString();
                    String n;
                    if ("".equals(jo.get("RemarkName").getAsString())) {
                        n = jo.get("NickName").getAsString();
                    } else {
                        n = jo.get("RemarkName").getAsString();
                    }
                    if (select().from(Member.class).where("userName", un).count() == 0) Anima.save(new Member(jo.get("UserName").getAsString(), n));
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void syncCheck() {
        String url = "https://webpush." + baseUrl.substring(8, baseUrl.length()) + "/synccheck";
        CloseableHttpResponse response = null;
        try {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("r", this.r()));
            list.add(new BasicNameValuePair("skey", skey));
            list.add(new BasicNameValuePair("sid", wxsid));
            list.add(new BasicNameValuePair("uin", wxuin));
            list.add(new BasicNameValuePair("deviceid", deviceId));
            list.add(new BasicNameValuePair("synckey", this.acquireSyncKeyStr()));
            list.add(new BasicNameValuePair("_", Long.toString(System.currentTimeMillis())));

            URIBuilder urlBuilder = new URIBuilder();
            urlBuilder.setPath(url);
            urlBuilder.setParameters(list);
            URI uri = urlBuilder.build();

            HttpGet httpGet = new HttpGet(uri);
            System.out.println("消息检查");
//            System.out.println(httpGet.getRequestLine());
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                System.out.println("ok");
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
//                System.out.println(responseContent);
                if (!TextUtils.isBlank(responseContent)) {
                    String retcodeStr = this.findStr("retcode:\"(\\d+)\"", responseContent);
                    String selectorStr = this.findStr("selector:\"(\\d+)\"}", responseContent);
                    if (!TextUtils.isBlank(retcodeStr) && !TextUtils.isBlank(selectorStr)) {
                        retcode = Integer.valueOf(retcodeStr);
                        selector = Integer.valueOf(selectorStr);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void listenMsg() {
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.syncCheck();
            if (retcode == 0) {
                if (selector == 0) {// 0为正常
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (selector == 2) {// 新消息
                    this.wxSync();
                }
            } else if (retcode == 1100) {// 1100暂不处理，再请求一次同步
                System.out.println(1100);
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String wxSync() {
        String msg = "";
        String reply = "";
        String url = baseUrl + "/webwxsync";
        CloseableHttpResponse response = null;
        try {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("sid", wxsid));
            list.add(new BasicNameValuePair("skey", skey));

            URIBuilder urlBuilder = new URIBuilder();
            urlBuilder.setPath(url);
            urlBuilder.setParameters(list);
            URI uri = urlBuilder.build();

            String json = "{\"BaseRequest\":{\"Uin\":\"" + wxuin + "\",\"Sid\":\"" + wxsid + "\",\"Skey\":\""
                    + skey + "\",\"DeviceID\":\"" + deviceId + "\"},\"SyncKey\":" + syncKeyJsonObject.toString()
                    + ",\"rr\":" + this.r() + "}";
            StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);

            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(stringEntity);
            System.out.println("获取最新消息");
//            System.out.println(httpPost.getRequestLine());
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String responseContent = EntityUtils.toString(entity, "UTF-8");
                // System.out.println(responseContent);
                if (!TextUtils.isBlank(responseContent)) {
                    JsonObject jsonObject = jsonParser.parse(responseContent).getAsJsonObject();
                    syncKeyJsonObject = jsonObject.getAsJsonObject("SyncKey");
                    JsonArray jsonArray = jsonObject.getAsJsonArray("AddMsgList");
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject obj = (JsonObject) jsonArray.get(i);
                        int msgType = obj.get("MsgType").getAsInt();
                        String fromUserName = obj.get("FromUserName").getAsString();
                        String toUserName = obj.get("ToUserName").getAsString();
                        String content = obj.get("Content").getAsString();
                        msg = content;
                        Long msgId = obj.get("MsgId").getAsLong();
                        Long createTime = obj.get("CreateTime").getAsLong();
                        Anima.save(new Message(msgId, createTime,
                                msgType, fromUserName, toUserName, content));
                        System.out.println("---");
                        if (fromUserName.equals(userName) && msgType != 51 && "jg".equals(content)) {
                            reply = "Bot开始接管～";
                            this.away = true;
                            this.awayTime = createTime;
                        } else if (fromUserName.equals(userName) && msgType != 51 && !"jg".equals(content) && !content.contains("add")) {
                            reply = "Bot下线～";
                            this.away = false;
                        } else if (fromUserName.equals(userName) && toUserName.contains("@@") && msgType != 51 && content.contains("add")) {
                            reply = "群组添加成功";
                            Anima.save(new Group(msgId, toUserName, content.substring(4)));
                        }
                        System.out.println(reply);
                        this.wxSendMsg(toUserName, reply);
                        if (msgType == 3) {// 图片
                            this.downImg(obj.get("MsgId").getAsLong(), 3);
//                            wxSendImg("filehelper", msgId);
                        }
                        if (msgType == 43 || msgType == 62) {// 视频
                            this.downImg(obj.get("MsgId").getAsLong(), 43);
                        }
                        if (specialUsers.contains(fromUserName)) {// 如果是特殊账号
                            System.out.println("special");
                        } else if (fromUserName.contains("@@")) {// 群消息
                            List<Group> groups = select().from(Group.class).all();
                            groups.forEach(group -> {
                                if (fromUserName.equals(group.getName())) {
                                    if (msgType == 10002) {
                                        this.wxSendMsg(fromUserName, "哈！撤回消息被我抓住了！");
                                        Message message = select().from(Message.class).where("id", findStr("msgid&gt;(\\d+)&lt;/msgid", content)).one();
                                        if (message.getType() == 1) {
                                            Member member = select().from(Member.class).where("username", message.getContent().substring(0, message.getContent().indexOf(":"))).one();
                                            this.wxSendMsg("filehelper", member.getName() + " - " + message.getContent().substring(message.getContent().indexOf("<br/>")));
                                        } else if (message.getType() == 3){
                                            System.out.println("img");
                                        }
                                    }
                                    if (content.contains("@" + group.getMe()) && this.away) {
                                        this.wxSendMsg(fromUserName, "Hi，" + group.getMe() + "已在" + (createTime - awayTime) / 60 +
                                                "分钟之前离开微信啦，我会督促他稍后回复你哦～");
                                    }
                                }
                            });

                        } else if (toUserName.equals(userName) && this.away) {// 必须是发给我的，我才回复
                            Chat chat = select().from(Chat.class).where("user", fromUserName).order("time desc").one();
                            if (chat == null) {
                                chat = new Chat(msgId, fromUserName, 0, System.currentTimeMillis());
                                Anima.save(chat);
                            }
                            if (msgType == 10002) {
                                reply = "哈！你偷偷撤回了什么？我全都看到了！赶快给我发个红包！不然我等下告诉我老大！";
//                                this.wxSendMsg(fromUserName, "哈！你偷偷撤回了什么？我全都看到了！赶快给我发个红包！不然我等下告诉我老大！");
                                Message message = select().from(Message.class).where("id", findStr("msgid&gt;(\\d+)&lt;/msgid", content)).one();
                                if (message.getType() == 1) {
                                    Member member = select().from(Member.class).where("username", fromUserName).one();
                                    this.wxSendMsg("filehelper", member.getName() + " - " + message.getContent());
                                }
                                else if (message.getType() == 3) System.out.println("img");
                            }
                            if (chat.getStatus() == 0) {
                                chat.setStatus(1);
                                chat.update();
                                switch (msgType){
                                    case 1: reply = "Hi，我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈";
//                                        this.wxSendMsg(fromUserName, "Hi，我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈"); break; // 普通消息
                                    case 3: reply = "Hi，这看起来是一张很重要的图片哦，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈";
//                                        this.wxSendMsg(fromUserName, "Hi，这看起来是一张很重要的图片哦，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈"); break; // 图片
                                    case 34: reply = "Hi，你的声音好好听呀，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈";
//                                        this.wxSendMsg(fromUserName, "Hi，你的声音好好听呀，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈"); break; // 语音
                                    case 42: reply = "Hi，看起来这是个很重要的名片哦，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈";
//                                            this.wxSendMsg(fromUserName, "Hi，看起来这是个很重要的名片哦，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈"); break; // 名片
                                    case 10000: if (content.contains("红包")) {
                                        reply = "哇，好大的红包！不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈";
//                                        this.wxSendMsg(fromUserName, "哇，好大的红包！不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈");
                                    } else if (content.contains("添加你为朋友")) {
                                        reply = "Hi，你好呀，很高兴认识你，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈";
//                                        this.wxSendMsg(fromUserName, "Hi，你好呀，很高兴认识你，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈");
                                    } break; // 系统消息
                                    default:
                                        reply = "Hi，你好像发了一个很神奇的东西，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈";
//                                        this.wxSendMsg(fromUserName, "Hi，你好像发了一个很神奇的东西，不过我老大已在" + (createTime - awayTime) / 60 + "分钟之前离开微信啦，我会督促他稍后回复你哦～你也可以试着跟我聊天哦哈哈"); break; // 其他
                                }
                            } else {
                                switch (msgType){
                                    case 1: reply = "目前我看不懂你写的什么诶，都怪我老大写不出什么像话的AI哈哈";
//                                        this.wxSendMsg(fromUserName, "目前我看不懂你写的什么诶，都怪我老大写不出什么像话的AI哈哈"); break; // 普通消息
                                    case 3: reply = "目前我看不懂你发的图片诶，都怪我老大写不出什么像话的AI哈哈";
//                                        this.wxSendMsg(fromUserName, "目前我看不懂你发的图片诶，都怪我老大写不出什么像话的AI哈哈"); break; // 图片
                                    case 34: reply = "目前我听不懂你的语音诶，不过声音很好听！";
//                                        this.wxSendMsg(fromUserName, "目前我听不懂你的语音诶，不过声音很好听！"); break; // 语音
                                    case 42: reply = "名片我先收下，待会要我老大去加他～";
//                                        this.wxSendMsg(fromUserName, "名片我先收下，待会要我老大去加他～"); break; // 名片
                                    case 10000: if (content.contains("红包")) {
                                        reply = "哇，偷偷发红包，谢谢老板！我先攒着，等我老大回来收～";
//                                        this.wxSendMsg(fromUserName, "哇，偷偷发红包，谢谢老板！我先攒着，等我老大回来收～");
                                    }  break; // 系统消息
                                    default: reply = "你好像发了一个很神奇的东西，不过目前我我不太懂诶，只能等我老大回来研究";
//                                        this.wxSendMsg(fromUserName, "你好像发了一个很神奇的东西，不过目前我我不太懂诶，只能等我老大回来研究"); break; // 其他
                                }
                            }
                            System.out.println(reply);
                            this.wxSendMsg(fromUserName, reply);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return msg;
    }


    private void checkState(String fromUserName, int msgType, String content){
        if (fromUserName.equals(userName) && "jg".equals(content)) this.away = true;
    }



    private void wxSendMsg(String toUserName, String msg) {
        if (TextUtils.isBlank(toUserName) || TextUtils.isBlank(msg)) {
            return;
        }
        String url = baseUrl + "/webwxsendmsg";
        CloseableHttpResponse response = null;
        try {
            String randomId = String.valueOf(System.currentTimeMillis());
            Random random = new Random();
            for (int i = 0; i < 4; i++) {
                randomId += random.nextInt(10);
            }
            String json = "{\"BaseRequest\":{\"Uin\":\"" + wxuin + "\",\"Sid\":\"" + wxsid + "\"," + "\"Skey\":\""
                    + skey + "\",\"DeviceID\":\"" + deviceId + "\"},\"Msg\":{\"Type\":1,\"Content\":\"" + msg
                    + "\",\"FromUserName\":\"" + userName + "\",\"ToUserName\":\"" + toUserName + "\",\"LocalID\":\""
                    + randomId + "\",\"ClientMsgId\":\"" + randomId + "\"},\"Scene\":0}";
            StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(stringEntity);
//            System.out.println(httpPost.getRequestLine());
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                System.out.println("给 " + toUserName + " 的消息 \"" +  msg + "\" 发送成功!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private void wxSendImg(String toUserName, Long imgId) {
//        String domain="wx2";
//        String webwxDataTicket = "";
//        String[] split = wechatMeta.getCookie().split(";");
//        for (String str : split) {
//            if(str.indexOf("webwx_data_ticket") != -1){
//                webwxDataTicket = (str.split("="))[1];
//            }
//        }
//        String passTicket =  wechatMeta.getPass_ticket();
//        String fromUserName = wechatMeta.getUser().getString("UserName");
//
//        String response = null;
//        InputStream inputStream = null;
//        InputStreamReader inputStreamReader = null;
//        BufferedReader bufferedReader = null;
//        HttpsURLConnection conn = null;
//        try {
//            File file = new File(filePath);
//            if (!file.exists() || !file.isFile()) {
//                throw new IOException("文件不存在");
//            }
//
//            //请求头参数
//            String boundary = "---------------------------16968206128770"; //区分每个参数之间
//            String freFix = "--";
//            String newLine = "\r\n";
//
//            URL urlObj = new URL("https://file."+domain+".qq.com/cgi-bin/mmwebwx-bin/webwxuploadmedia?f=json");
//            conn = (HttpsURLConnection) urlObj.openConnection();
//            conn.setRequestMethod("3");
//            conn.setDoOutput(true);
//            conn.setDoInput(true);
//            conn.setConnectTimeout(5000);
//            conn.setUseCaches(false);
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Host", "file."+domain+".qq.com");
//            conn.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0");
//            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
//            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
//            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
//            conn.setRequestProperty("Referer", "https://."+domain+".qq.com/?&lang=zh_CN");
//            conn.setRequestProperty("Content-Length", Long.toString(file.length()));
//            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);
//            conn.setRequestProperty("origin", "https://."+domain+"..qq.com/");
//            conn.setRequestProperty("Connection", "Keep-Alive");
//            conn.setRequestProperty("Cookie", wechatMeta.getCookie());
//
//            // 请求主体
//            StringBuffer sb = new StringBuffer();
//
//            sb.append(freFix+boundary).append(newLine); //这里注意多了个freFix，来区分去请求头中的参数
//            sb.append("Content-Disposition: form-data; name=\"id\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append("WU_FILE_1").append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"name\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append(file.getName()).append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"type\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append("image/jpeg").append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"lastModifiedDate\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append("Tue Feb 14 2017 22:07:03 GMT+0800").append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"size\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append(file.length()).append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"mediatype\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append("pic").append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"uploadmediarequest\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append("{\"UploadType\":2,\"BaseRequest\":{\"Uin\":"+this.wxuin+",\"Sid\":\""+this.wxsid+"\",\"Skey\":\""+this.skey+"\",\"DeviceID\":\""+deviceid+StringKit.getRandomNumber(5)+"\"},\"ClientMediaId\":"+System.currentTimeMillis()+",\"TotalLen\":"+file.length()+",\"StartPos\":0,\"DataLen\":"+file.length()+",\"MediaType\":4,\"FromUserName\":\""+fromUserName+"\",\"ToUserName\":\""+toUserName+"\",\"FileMd5\":\"7a392dfff5a45cc29d494434a1fbaf15\"}").append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"webwx_data_ticket\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append(webwxDataTicket).append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"pass_ticket\"").append(newLine);
//            sb.append(newLine);
//
//            sb.append(passTicket).append(newLine);
//            sb.append(freFix+boundary).append(newLine);
//            sb.append("Content-Disposition: form-data; name=\"filename\"; filename=\""+file.getName()+"\"").append(newLine);
//            sb.append("Content-Type: application/octet-stream");
//            sb.append(newLine);
//            sb.append(newLine);
//
//            // System.out.println(sb.toString());
//            //FileOutputStream writer = new FileOutputStream(new File("e:\\img\\Resulsssst.txt"));
//            OutputStream outputStream = new DataOutputStream(conn.getOutputStream());
//            outputStream.write(sb.toString().getBytes("utf-8"));//写入请求参数
//
//            DataInputStream dis = new DataInputStream(new FileInputStream(file));
//            int bytes = 0;
//            byte[] bufferOut = new byte[1024];
//            while ((bytes = dis.read(bufferOut)) != -1) {
//                outputStream.write(bufferOut,0,bytes);//写入图片
//            }
//            outputStream.write(newLine.getBytes());
//            outputStream.write((freFix+boundary+freFix+newLine).getBytes("utf-8"));//标识请求数据写入结束
//            dis.close();
//            outputStream.close();
//            //读取响应信息
//            inputStream = conn.getInputStream();
//            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
//            bufferedReader = new BufferedReader(inputStreamReader);
//            String str = null;
//            StringBuffer buffer = new StringBuffer();
//            while ((str = bufferedReader.readLine()) != null) {
//                buffer.append(str);
//            }
//            response = buffer.toString();
//            System.out.println("response++++++++"+response);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }finally{
//            if(conn!=null){
//                conn.disconnect();
//            }
//            try {
//                bufferedReader.close();
//                inputStreamReader.close();
//                inputStream.close();
//            } catch (IOException execption) {
//
//            }
//        }
//        return response;
//    }

    private String generateMsg(String fromUserName, String content) {

        String msg = "";
        if (content.contains("你好")||content.contains("hi")||content.contains("Hi")) msg="你好呀";
        else if (content.contains("你是谁")) msg="我老大出去了，我是他的的机器人～";
        else if (content.contains("你在干嘛")) msg="我在等你消息呀";
        else if (content.contains("再见")||content.contains("拜拜")||content.contains("bye")||content.contains("Bye")) msg="拜拜，明天见哦";
        else msg="听不懂你在说什么，等我老大回来跟你说";
        System.out.println(msg);
        return msg;
    }

    private String r() {
        return Integer.toString(~((int) System.currentTimeMillis()));// 转换成int后取反，因为js位运算只支持32位
    }

    private String acquireSyncKeyStr() {
        String syncKeyTmp = "";
        JsonArray jsonArray = syncKeyJsonObject.getAsJsonArray("List");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = (JsonObject) jsonArray.get(i);
            syncKeyTmp += obj.get("Key") + "_" + obj.get("Val") + "|";
        }
        if (syncKeyTmp.length() > 0) {
            syncKeyTmp = syncKeyTmp.substring(0, syncKeyTmp.length() - 1);
        }
        return syncKeyTmp;
    }

    private String findStr(String regex, String str) {
        Matcher matcher = Pattern.compile(regex).matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
