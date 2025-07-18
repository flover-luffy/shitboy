package net.luffy.handler;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.luffy.Newboy;
import okhttp3.Headers;
import okhttp3.Request;
import net.luffy.model.Pocket48Message;
import net.luffy.model.Pocket48RoomInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Pocket48Handler extends AsyncWebHandlerBase {

    public static final String ROOT = "https://pocketapi.48.cn";
    public static final String SOURCEROOT = "https://source.48.cn/";
    public static final String APIAnswerDetail = ROOT + "/idolanswer/api/idolanswer/v1/question_answer/detail";
    public static final List<Long> voidRoomVoiceList = new ArrayList<>();
    private static final String APILogin = ROOT + "/user/api/v1/login/app/mobile";
    private static final String APIBalance = ROOT + "/user/api/v1/user/money";
    private static final String APIStar2Server = ROOT + "/im/api/v1/im/server/jump";
    private static final String APIServer2Channel = ROOT + "/im/api/v1/team/last/message/get";
    private static final String APIChannel2Server = ROOT + "/im/api/v1/im/team/room/info";
    private static final String APISearch = ROOT + "/im/api/v1/im/server/search";
    private static final String APIMsgOwner = ROOT + "/im/api/v1/team/message/list/homeowner";
    private static final String APIMsgAll = ROOT + "/im/api/v1/team/message/list/all";
    private static final String APIUserInfo = ROOT + "/user/api/v1/user/info/home";
    private static final String APIUserArchives = ROOT + "/user/api/v1/user/star/archives";
    private static final String APILiveList = ROOT + "/live/api/v1/live/getLiveList";
    private static final String APIRoomVoice = ROOT + "/im/api/v1/team/voice/operate";
    private final Pocket48HandlerHeader header;
    private final HashMap<Long, String> name = new HashMap<>();

    public Pocket48Handler() {
        super();
        this.header = new Pocket48HandlerHeader(properties);
    }

    public static final String getOwnerOrTeamName(Pocket48RoomInfo roomInfo) {
        switch (String.valueOf(roomInfo.getSeverId())) {
            case "1148749":
                return "TEAM Z";
            case "1164313":
                return "TEAM X";
            case "1164314":
                return "TEAM NIII";
            case "1181051":
                return "TEAM HII";
            case "1181256":
                return "TEAM G";
            case "1213978":
                return "TEAM NII";
            case "1214171":
                return "TEAM SII";
            case "1115226":
                return "CKG48";
            case "1115413":
                return "BEJ48";
            default:
                return roomInfo.getOwnerName();
        }
    }

    //登陆前
    public boolean login(String account, String password) {
        try {
            String url = APILogin;
            
            JSONObject requestBody = new JSONObject();
            requestBody.set("pwd", password);
            requestBody.set("mobile", account);
            
            String s = post(url, requestBody.toString());
            
            JSONObject object = JSONUtil.parseObj(s);
            if (object.getInt("status") == 200) {
                JSONObject content = JSONUtil.parseObj(object.getObj("content"));
                login(content.getStr("token"), true);
                return true;
            } else {
                logInfo("口袋48登陆失败：" + object.getStr("message"));
                return false;
            }
            
        } catch (Exception e) {
            logError("登录异常: " + e.getMessage());
            return false;
        }
    }

    public void login(String token, boolean save) {
        header.setToken(token);
        logInfo("口袋48登陆成功");
        if (save)
            Newboy.INSTANCE.getConfig().setAndSaveToken(token);
    }

    public boolean isLogin() {
        return header.getToken() != null;
    }

    /* ----------------------文字获取类---------------------- */

    public void logout() {
        header.setToken(null);
    }

    //掉token时重新登陆一下，因为没传入code参数，就写在这咯~
    @Override
    protected void logError(String msg) {
        if (msg.endsWith("非法授权")) {
            logout();
            String token_local = Newboy.INSTANCE.getConfig().getToken();
            if (!properties.pocket48_token.equals(token_local)) {
                login(token_local, false);
            } else if (properties.pocket48_account.equals("") || properties.pocket48_password.equals("")) {
                super.logError("口袋48 token失效请重新填写，同时填写token和账密可在token时效时登录（优先使用token）");
            } else {
                login(properties.pocket48_account, properties.pocket48_password);
            }
        }

        super.logError(msg);
    }

    // 重写buildRequest方法以添加Token头
    @Override
    protected Request.Builder buildRequest(String url) {
        Request.Builder builder = super.buildRequest(url)
                .addHeader("Content-Type", "application/json;charset=utf-8")
                .addHeader("Host", "pocketapi.48.cn")
                .addHeader("pa", "MTc1MTg5NTgzMjAwMCwzODMzLEQ3ODVBRENBM0U3QTkzRDVFNTJCMjVDQUJDRUY4NDczLA==")
                .addHeader("User-Agent", "PocketFans201807/7.1.34 (iPhone; iOS 19.0; Scale/2.00)")
                .addHeader("appInfo", "{\"vendor\":\"apple\",\"deviceId\":\"8D6DDD0B-2233-4622-89AA-AABB14D4F37B\",\"appVersion\":\"7.1.34\",\"appBuild\":\"25060602\",\"osVersion\":\"19.0\",\"osType\":\"ios\",\"deviceName\":\"iPhone 11\",\"os\":\"ios\"}");
        
        // 如果已登录，添加token头
        if (header.getToken() != null) {
            builder.addHeader("token", header.getToken());
        }
        
        return builder;
    }

    public String getBalance() {
        try {
            String url = "https://pocketapi.48.cn/user/api/v1/user/info/pfid";
            
            Headers headers = new Headers.Builder()
                .add("token", header.getToken())
                .add("User-Agent", "PocketFans201807/6.0.16 (iPhone; iOS 13.5.1; Scale/2.00)")
                .add("Accept", "application/json")
                .add("Accept-Language", "zh-Hans-CN;q=1")
                .build();
            
            String response = get(url, headers);
            
            JSONObject jsonResponse = JSONUtil.parseObj(response);
            if (jsonResponse.getInt("status") == 200) {
                JSONObject content = jsonResponse.getJSONObject("content");
                return content.getStr("pfid", "0");
            } else {
                logError("获取余额失败: " + jsonResponse.getStr("message"));
                return "0";
            }
            
        } catch (Exception e) {
            logError("获取余额异常: " + e.getMessage());
            return "0";
        }
    }

    public String getStarNameByStarID(long starID) {
        if (name.containsKey(starID))
            return name.get(starID);

        JSONObject info = getUserInfo(starID);
        if (info == null)
            return null;

        Object starName = info.getObj("starName");
        String starName_ = starName == null ? "" : (String) starName;
        name.put(starID, starName_);
        return starName_;
    }

    public JSONObject getUserInfo(long starID) {
        String s = post(APIUserInfo, String.format("{\"userId\":%d}", starID));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            return JSONUtil.parseObj(content.getObj("baseUserInfo"));

        } else {
            logError(starID + object.getStr("message"));
        }
        return null;

    }

    public JSONObject getUserArchives(long starID) {
        String s = post(APIUserArchives, String.format("{\"memberId\":%d}", starID));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            return JSONUtil.parseObj(object.getObj("content"));

        } else {
            logError(starID + object.getStr("message"));
        }
        return null;
    }

    public String getUserNickName(long id) {
        try {
            return getUserInfo(id).getStr("nickname");
        } catch (Exception e) {
            e.printStackTrace();
            return "null";
        }
    }

    private JSONObject getJumpContent(long starID) {
        String s = post(APIStar2Server, String.format("{\"starId\":%d,\"targetType\":1}", starID));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            return JSONUtil.parseObj(object.getObj("content"));

        } else {
            logError(starID + object.getStr("message"));
        }
        return null;
    }

    public long getMainChannelIDByStarID(long starID) {
        JSONObject content = getJumpContent(starID);
        if (content != null) {
            Long id = content.getLong("channelId");
            if (id != null) {
                return id;
            }
        }
        return 0;
    }

    public Long getServerIDByStarID(long starID) {
        JSONObject content = getJumpContent(starID);
        if (content != null) {
            JSONObject serverInfo = JSONUtil.parseObj(content.getObj("jumpServerInfo"));
            if (serverInfo != null) {
                return serverInfo.getLong("serverId");
            }
        }
        return null;
    }

    public Long[] getChannelIDBySeverID(long serverID) {
        String s = post(APIServer2Channel, String.format("{\"serverId\":\"%d\"}", serverID));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            List<Long> rs = new ArrayList<>();
            for (Object room : content.getBeanList("lastMsgList", Object.class)) {
                rs.add(JSONUtil.parseObj(room).getLong("channelId"));
            }
            return rs.toArray(new Long[0]);

        } else {
            logError(serverID + object.getStr("message"));
            return new Long[0];
        }
    }

    public Pocket48RoomInfo getRoomInfoByChannelID(long roomID) {
        String s = post(APIChannel2Server, String.format("{\"channelId\":\"%d\"}", roomID));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            JSONObject roomInfo = JSONUtil.parseObj(content.getObj("channelInfo"));
            return new Pocket48RoomInfo(roomInfo);

        } else if (object.getInt("status") == 2001
                && object.getStr("message").indexOf("question") != -1) { //只有配置中存有severID的加密房间会被解析
            JSONObject message = JSONUtil.parseObj(object.getObj("message"));
            return new Pocket48RoomInfo.LockedRoomInfo(message.getStr("question") + "？",
                    properties.pocket48_serverID.get(roomID), roomID);
        } else {
            logError(roomID + object.getStr("message"));
        }
        return null;

    }

    public Object[] search(String content_) {
        String s = post(APISearch, String.format("{\"searchContent\":\"%s\"}", content_));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            if (content.containsKey("serverApiList")) {
                JSONArray a = content.getJSONArray("serverApiList");
                return a.stream().toArray();
            }

        } else {
            logError(object.getStr("message"));
        }
        return new Object[0];
    }

    public String getAnswerNameTo(String answerID, String questionID) {
        String s = post(APIAnswerDetail, String.format("{\"answerId\":\"%s\",\"questionId\":\"%s\"}", answerID, questionID));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            return content.getStr("userName");

        } else {
            logError(object.getStr("message"));
        }
        return null;
    }

    /* ----------------------房间类---------------------- */
    //获取最新消息并整理成Pocket48Message[]
    //注意endTime中无key=roomInfo.getRoomId()时此方法返回null
    //采用13位时间戳
    public Pocket48Message[] getMessages(Pocket48RoomInfo roomInfo, HashMap<Long, Long> endTime) {
        long roomID = roomInfo.getRoomId();
        if (!endTime.containsKey(roomID))
            return null;

        List<Object> msgs = getOriMessages(roomID, roomInfo.getSeverId());
        if (msgs != null) {
            List<Pocket48Message> rs = new ArrayList<>();
            long latest = 0;
            for (Object message : msgs) {
                JSONObject m = JSONUtil.parseObj(message);
                long time = m.getLong("msgTime");

                if (endTime.get(roomID) >= time)
                    break; //api有时间次序

                if (latest < time) {
                    latest = time;
                }

                rs.add(Pocket48Message.construct(
                        roomInfo,
                        m
                ));
            }
            if (latest != 0)
                endTime.put(roomID, latest);
            return rs.toArray(new Pocket48Message[0]);
        }
        return new Pocket48Message[0];
    }

    public Pocket48Message[] getMessages(long roomID, HashMap<Long, Long> endTime) {
        Pocket48RoomInfo roomInfo = getRoomInfoByChannelID(roomID);
        if (roomInfo != null) {
            return getMessages(roomInfo, endTime);
        }
        return new Pocket48Message[0];
    }

    //获取全部消息并整理成Pocket48Message[]
    public Pocket48Message[] getMessages(Pocket48RoomInfo roomInfo) {
        long roomID = roomInfo.getRoomId();
        List<Object> msgs = getOriMessages(roomID, roomInfo.getSeverId());
        if (msgs != null) {
            List<Pocket48Message> rs = new ArrayList<>();
            for (Object message : msgs) {
                rs.add(Pocket48Message.construct(
                        roomInfo,
                        JSONUtil.parseObj(message)
                ));
            }
            return rs.toArray(new Pocket48Message[0]);
        }

        return new Pocket48Message[0];
    }

    public Pocket48Message[] getMessages(long roomID) {
        Pocket48RoomInfo roomInfo = getRoomInfoByChannelID(roomID);
        if (roomInfo != null) {
            return getMessages(roomInfo);
        }

        return new Pocket48Message[0];
    }

    //获取未整理的消息
    private List<Object> getOriMessages(long roomID, long serverID) {
        String s = post(APIMsgOwner, String.format("{\"nextTime\":0,\"serverId\":%d,\"channelId\":%d,\"limit\":100}", serverID, roomID));
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            List<Object> out = content.getBeanList("message", Object.class);
            out.sort((a, b) -> JSONUtil.parseObj(b).getLong("msgTime") - JSONUtil.parseObj(a).getLong("msgTime") > 0 ? 1 : 0);//口袋消息好像会乱（？）
            return out;

        } else {
            logError(roomID + object.getStr("message"));

        }
        return null;
    }

    public List<Long> getRoomVoiceList(long roomID, long serverID) {
        String s = post(APIRoomVoice, String.format("{\"channelId\":%d,\"serverId\":%d,\"operateCode\":2}", roomID, serverID));
        JSONObject object = JSONUtil.parseObj(s);
        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            JSONArray a = content.getJSONArray("voiceUserList");
            List<Long> l = new ArrayList<>();
            if (a.size() > 0) {
                for (Object star_ : a.stream().toArray()) {
                    JSONObject star = JSONUtil.parseObj(star_);
                    long starID = star.getLong("userId");
                    l.add(starID);

                    //优化：names的另一种添加途径
                    if (!name.containsKey(starID)) {
                        name.put(starID, star.getStr("nickname"));
                    }
                }
                return l;
            }

        } else {
            logError(roomID + object.getStr("message"));
        }
        return voidRoomVoiceList;
    }

    /* ----------------------直播类---------------------- */
    public List<Object> getLiveList() {
        String s = post(APILiveList, "{\"groupId\":0,\"debug\":true,\"next\":0,\"record\":false}");
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            return content.getBeanList("liveList", Object.class);

        } else {
            logError(object.getStr("message"));
        }
        return null;

    }

    public List<Object> getRecordList() {
        String s = post(APILiveList, "{\"groupId\":0,\"debug\":true,\"next\":0,\"record\":true}");
        JSONObject object = JSONUtil.parseObj(s);

        if (object.getInt("status") == 200) {
            JSONObject content = JSONUtil.parseObj(object.getObj("content"));
            return content.getBeanList("liveList", Object.class);

        } else {
            logError(object.getStr("message"));
        }
        return null;

    }

}
