package net.luffy.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.Setting;
import net.luffy.Newboy;
import net.luffy.model.Pocket48Subscribe;
import net.luffy.model.WeidianCookie;
import net.luffy.util.SubscriptionConfig;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigOperator {

    private static ConfigOperator instance;
    private Setting setting;
    private Properties properties;
    
    /**
     * 获取ConfigOperator单例实例
     */
    public static ConfigOperator getInstance() {
        if (instance == null) {
            synchronized (ConfigOperator.class) {
                if (instance == null) {
                    instance = new ConfigOperator();
                }
            }
        }
        return instance;
    }

    public void load(Properties properties) {
        this.properties = properties;
        File file = properties.configData;
        if (!file.exists()) {
            FileUtil.touch(file);
            // 创建Setting对象并设置基本的默认配置
            Setting tempSetting = new Setting(file, StandardCharsets.UTF_8, false);
            
            // 设置基本配置的默认值
            tempSetting.setByGroup("basic", "enable", "true");
            tempSetting.setByGroup("basic", "save_login", "false");
            tempSetting.setByGroup("basic", "admins", "123456789");
            tempSetting.setByGroup("basic", "secureGroup", "");
            
            // 设置定时任务默认配置 - 优化为5秒间隔，减少系统负载
            tempSetting.setByGroup("schedule", "pocket48", "*/5 * * * * *");
            tempSetting.setByGroup("schedule", "weibo", "*/5 * * * *");
            // onlineStatus定时任务已迁移到异步监控系统
            tempSetting.setByGroup("schedule_order", "weidian", "*/2 * * * *");
            tempSetting.setByGroup("schedule_item", "weidian", "*/5 * * * *");
            
            // 设置口袋48默认配置
            tempSetting.setByGroup("pocket48", "account", "");
            tempSetting.setByGroup("pocket48", "password", "");
            tempSetting.setByGroup("pocket48", "token", "");
            
            tempSetting.setByGroup("subscribe", "async_monitor", "[]");
            
            // 设置异步监控配置默认值
            tempSetting.setByGroup("async_monitor", "schedule_pattern", "*/30 * * * * *");
            
            // 设置消息延迟优化配置默认值
            tempSetting.setByGroup("message_delay", "optimization_mode", "BALANCED");
            tempSetting.setByGroup("message_delay", "text", "12");
            tempSetting.setByGroup("message_delay", "media", "25");
            tempSetting.setByGroup("message_delay", "group_high_priority", "15");
            tempSetting.setByGroup("message_delay", "group_low_priority", "25");
            tempSetting.setByGroup("message_delay", "processing_timeout", "15");
            tempSetting.setByGroup("message_delay", "high_load_multiplier", "1.0");
            tempSetting.setByGroup("message_delay", "critical_load_multiplier", "2.0");
            
            // 设置订阅配置默认值
            tempSetting.setByGroup("subscribe", "pocket48", "[]");
            tempSetting.setByGroup("subscribe", "weibo", "[]");
            // onlineStatus订阅已迁移到异步监控系统
            
            // 设置商店配置默认值
            tempSetting.setByGroup("shops", "weidian", "[]");
            
            // 保存默认配置
            tempSetting.store();
            
            Newboy.INSTANCE.getLogger().info("首次加载已生成 config/net.luffy.newboy/config.setting 配置文件");
            Newboy.INSTANCE.getLogger().info("请根据需要修改配置文件后重启插件");
            Newboy.INSTANCE.getLogger().info("配置文件路径: config/net.luffy.newboy/config.setting");
        }

        this.setting = new Setting(file, StandardCharsets.UTF_8, false);
        init();
    }

    public Setting getSetting() {
        return setting;
    }

    public void init() {
        properties.enable = setting.getBool("basic", "enable", true);
        properties.save_login = setting.getBool("basic", "save_login", false);
        properties.admins = setting.getStrings("basic", "admins");
        properties.secureGroup = setting.getStrings("basic", "secureGroup");
        if (properties.admins == null)
            properties.admins = new String[]{};
        if (properties.secureGroup == null)
            properties.secureGroup = new String[]{};

        // 进群欢迎功能已移除

        //schedule pattern
        properties.pocket48_pattern = setting.getStr("schedule", "pocket48", "* * * * *");

        properties.weibo_pattern = setting.getStr("schedule", "weibo", "*/5 * * * *");
        properties.weidian_pattern_order = setting.getStr("schedule_order", "weidian", "*/2 * * * *");
        properties.weidian_pattern_item = setting.getStr("schedule_item", "weidian", "*/5 * * * *");
        // 在线状态监控配置已迁移到异步监控系统
        
        // 异步监控配置
        properties.async_monitor_schedule_pattern = setting.getStr("async_monitor", "schedule_pattern", "*/30 * * * * *");
        
        // 消息延迟优化配置
        properties.message_delay_optimization_mode = setting.getStr("message_delay", "optimization_mode", "BALANCED");
        properties.message_delay_text = setting.getInt("message_delay", "text", 12);
        properties.message_delay_media = setting.getInt("message_delay", "media", 25);
        properties.message_delay_group_high_priority = setting.getInt("message_delay", "group_high_priority", 15);
        properties.message_delay_group_low_priority = setting.getInt("message_delay", "group_low_priority", 25);
        properties.message_delay_processing_timeout = setting.getInt("message_delay", "processing_timeout", 15);
        properties.message_delay_high_load_multiplier = setting.getDouble("message_delay", "high_load_multiplier", 1.0);
        properties.message_delay_critical_load_multiplier = setting.getDouble("message_delay", "critical_load_multiplier", 2.0);

        //口袋48
        properties.pocket48_account = setting.getStr("pocket48", "account", "");
        properties.pocket48_password = setting.getStr("pocket48", "password", "");
        properties.pocket48_token = setting.getStr("pocket48", "token", "");
        
        // 配置验证日志
        if (!properties.pocket48_token.isEmpty()) {
            Newboy.INSTANCE.getLogger().info("口袋48 Token 配置已读取，长度: " + properties.pocket48_token.length());
        } else if (!properties.pocket48_account.isEmpty() && !properties.pocket48_password.isEmpty()) {
            Newboy.INSTANCE.getLogger().info("口袋48账号密码配置已读取");
        } else {
            Newboy.INSTANCE.getLogger().warning("口袋48未配置登录信息，请在config.setting中配置token或账号密码");
        }

        for (Object a :
                JSONUtil.parseArray(setting.getByGroup("subscribe", "pocket48")).toArray()) {
            JSONObject sub = JSONUtil.parseObj(a);
            @SuppressWarnings("unchecked")
            List<Long> rooms = (List<Long>) sub.getBeanList("roomSubs", Long.class);
            @SuppressWarnings("unchecked")
            List<Long> stars = (List<Long>) sub.getBeanList("starSubs", Long.class);

            properties.pocket48_subscribe
                    .put(sub.getLong("qqGroup"),
                            new Pocket48Subscribe(
                                    sub.getBool("showAtOne", true),
                                    rooms == null ? new ArrayList<>() : rooms,
                                    stars == null ? new ArrayList<>() : stars
                            ));
        }

        //口袋48房间连接 - 使用高性能JSON解析
        Object[] pocket48Array = JSONUtil.parseArray(setting.getByGroup("roomConnection", "pocket48")).toArray();
        for (Object a : pocket48Array) {
            JSONObject sid = (a instanceof JSONObject) ? (JSONObject) a : JSONUtil.parseObj(a);
            properties.pocket48_serverID.put(sid.getLong("roomID"), sid.getLong("serverID"));
        }



        //微博 - 使用批量解析优化
        Object[] weiboArray = JSONUtil.parseArray(setting.getByGroup("subscribe", "weibo")).toArray();
        for (Object a : weiboArray) {
            JSONObject subs = (a instanceof JSONObject) ? (JSONObject) a : JSONUtil.parseObj(a);

            long g = subs.getLong("qqGroup");
            @SuppressWarnings("unchecked")
            List<Long> userSubs = (List<Long>) subs.getBeanList("userSubs", Long.class);
            properties.weibo_user_subscribe.put(g, userSubs == null ? new ArrayList<>() : userSubs);

            @SuppressWarnings("unchecked")
            List<String> sTopicSubs = (List<String>) subs.getBeanList("superTopicSubs", String.class);
            properties.weibo_superTopic_subscribe.put(g, sTopicSubs == null ? new ArrayList<>() : sTopicSubs);

        }

        //微店 - 使用高性能JSON解析
        Object[] weidianArray = JSONUtil.parseArray(setting.getByGroup("shops", "weidian")).toArray();
        for (Object a : weidianArray) {
            JSONObject shop = (a instanceof JSONObject) ? (JSONObject) a : JSONUtil.parseObj(a);

            long g = shop.getLong("qqGroup");
            String cookie = shop.getStr("cookie", "");
            boolean autoDeliver = shop.getBool("autoDeliver", false);
            boolean doBroadCast = shop.getBool("doBroadCast", true);
            List<Long> highlight = shop.getBeanList("highlight", Long.class);
            List<Long> shielded = shop.getBeanList("shielded", Long.class);
            properties.weidian_cookie.put(g, WeidianCookie.construct(cookie, autoDeliver, doBroadCast,
                    highlight == null ? new ArrayList<>() : highlight,
                    shielded == null ? new ArrayList<>() : shielded));

        }
        
        // 在线状态监控配置已迁移到异步监控系统，不再从此处加载
    }

    //修改配置并更新缓存的方法
    public void swch(boolean on) {
        setting.setByGroup("basic", "enable", String.valueOf(on));
        safeStoreConfig("基础配置");
        properties.enable = setting.getBool("basic", "enable", true);
    }

    // 进群欢迎功能已移除

    public boolean setAndSaveToken(String token) {
        properties.pocket48_token = token;
        setting.setByGroup("pocket48", "token", token);
        safeStoreConfig("口袋48 Token配置");
        return true;
    }

    public String getToken() {
        return setting.getStr("pocket48", "token", properties.pocket48_token);
    }

    public boolean addPocket48RoomSubscribe(long room_id, long group) {
        if (!properties.pocket48_subscribe.containsKey(group)) {
            properties.pocket48_subscribe.put(group, new Pocket48Subscribe(
                    true, new ArrayList<>(), new ArrayList<>()
            ));
        }

        if (properties.pocket48_subscribe.get(group).getRoomIDs().contains(room_id))
            return false;

        properties.pocket48_subscribe.get(group).getRoomIDs().add(room_id);
        savePocket48SubscribeConfig();
        return true;
    }

    public boolean rmPocket48RoomSubscribe(long room_id, long group) {
        if (!properties.pocket48_subscribe.get(group).getRoomIDs().contains(room_id))
            return false;

        properties.pocket48_subscribe.get(group).getRoomIDs().remove(room_id);
        savePocket48SubscribeConfig();
        return true;
    }

    public boolean addRoomIDConnection(long room_id, long sever_id) {
        if (properties.pocket48_serverID.containsKey(room_id))
            return false;

        properties.pocket48_serverID.put(room_id, sever_id);
        savePocket48RoomIDConnectConfig();
        return true;
    }

    public boolean rmRoomIDConnection(long room_id, long sever_id) {
        if (!properties.pocket48_serverID.containsKey(room_id))
            return false;

        properties.pocket48_serverID.remove(room_id, sever_id);
        savePocket48RoomIDConnectConfig();
        return true;
    }




    public boolean addWeiboUserSubscribe(long id, long group) {
        if (!properties.weibo_user_subscribe.containsKey(group)) {
            properties.weibo_user_subscribe.put(group, new ArrayList<>());
            properties.weibo_superTopic_subscribe.put(group, new ArrayList<>());
        }

        if (properties.weibo_user_subscribe.get(group).contains(id))
            return false;

        properties.weibo_user_subscribe.get(group).add(id);
        saveWeiboConfig();
        return true;
    }

    public boolean rmWeiboUserSubscribe(long id, long group) {
        if (!properties.weibo_user_subscribe.get(group).contains(id))
            return false;

        properties.weibo_user_subscribe.get(group).remove(id);
        saveWeiboConfig();
        return true;
    }

    public boolean addWeiboSTopicSubscribe(String id, long group) {
        if (!properties.weibo_user_subscribe.containsKey(group)) {
            properties.weibo_user_subscribe.put(group, new ArrayList<>());
            properties.weibo_superTopic_subscribe.put(group, new ArrayList<>());
        }

        if (properties.weibo_superTopic_subscribe.get(group).contains(id))
            return false;

        properties.weibo_superTopic_subscribe.get(group).add(id);
        saveWeiboConfig();
        return true;
    }

    public boolean rmWeiboSTopicSubscribe(String id, long group) {
        if (!properties.weibo_superTopic_subscribe.get(group).contains(id))
            return false;

        properties.weibo_superTopic_subscribe.get(group).remove(id);
        saveWeiboConfig();
        return true;
    }

    public boolean setWeidianCookie(String cookie, long group) {
        boolean autoDeliver = false;
        boolean doBroadcast = true;
        List<Long> highlightItem = new ArrayList<>();
        List<Long> shieldItem = new ArrayList<>();
        if (properties.weidian_cookie.containsKey(group)) {
            autoDeliver = properties.weidian_cookie.get(group).autoDeliver;
            doBroadcast = properties.weidian_cookie.get(group).doBroadcast;
            highlightItem = properties.weidian_cookie.get(group).highlightItem;
            shieldItem = properties.weidian_cookie.get(group).shieldedItem;
        }
        properties.weidian_cookie.put(group, WeidianCookie.construct(cookie, autoDeliver, doBroadcast, highlightItem, shieldItem));
        saveWeidianConfig();
        return true;
    }

    public int switchWeidianAutoDeliver(long group) {
        if (!properties.weidian_cookie.containsKey(group))
            return -1;

        WeidianCookie cookie = properties.weidian_cookie.get(group);
        cookie.autoDeliver = !cookie.autoDeliver;
        saveWeidianConfig();
        return cookie.autoDeliver ? 1 : 0;
    }

    public int switchWeidianDoBroadCast(long group) {
        if (!properties.weidian_cookie.containsKey(group))
            return -1;

        WeidianCookie cookie = properties.weidian_cookie.get(group);
        cookie.doBroadcast = !cookie.doBroadcast;
        saveWeidianConfig();
        return cookie.doBroadcast ? 1 : 0;
    }

    public boolean rmWeidianCookie(long group) {
        if (!properties.weidian_cookie.containsKey(group)) {
            return false;
        }

        properties.weidian_cookie.remove(group);
        saveWeidianConfig();
        return true;
    }

    public int highlightWeidianItem(long group, long itemid) {
        if (!properties.weidian_cookie.containsKey(group)) {
            return -1;
        }

        List<Long> it = properties.weidian_cookie.get(group).highlightItem;
        if (it.contains(itemid)) {
            it.remove(itemid);
        } else {
            it.add(itemid);
        }
        saveWeidianConfig();
        return it.contains(itemid) ? 1 : 0;
    }

    public int shieldWeidianItem(long group, long itemid) {
        if (!properties.weidian_cookie.containsKey(group)) {
            return -1;
        }

        List<Long> it = properties.weidian_cookie.get(group).shieldedItem;
        if (it.contains(itemid)) {
            it.remove(itemid);
        } else {
            it.add(itemid);
        }
        saveWeidianConfig();
        return it.contains(itemid) ? 1 : 0;
    }

    // 进群欢迎功能已移除

    public void savePocket48SubscribeConfig() {
        String a = "[";
        for (long group : properties.pocket48_subscribe.keySet()) {
            JSONObject object = new JSONObject();
            Pocket48Subscribe subscribe = properties.pocket48_subscribe.get(group);
            object.set("qqGroup", group);
            object.set("showAtOne", subscribe.showAtOne());
            object.set("starSubs", subscribe.getStarIDs().toArray());
            object.set("roomSubs", subscribe.getRoomIDs().toArray());
            a += object + ",";
        }
        setting.setByGroup("subscribe", "pocket48", (a.length() > 1 ? a.substring(0, a.length() - 1) : a) + "]");
        
        // 安全保存配置，避免覆盖整个配置文件
        safeStoreConfig("口袋48订阅配置");
    }

    public void savePocket48RoomIDConnectConfig() {
        String a = "[";
        for (long room_id : properties.pocket48_serverID.keySet()) {
            JSONObject object = new JSONObject();
            object.set("roomID", room_id);
            object.set("serverID", properties.pocket48_serverID.get(room_id));
            a += object + ",";
        }
        setting.setByGroup("roomConnection", "pocket48", (a.length() > 1 ? a.substring(0, a.length() - 1) : a) + "]");
        safeStoreConfig("口袋48房间连接配置");
    }



    public void saveWeiboConfig() {
        String a = "[";
        for (long group : properties.weibo_user_subscribe.keySet()) {
            JSONObject object = new JSONObject();
            object.set("qqGroup", group);
            object.set("userSubs", properties.weibo_user_subscribe.get(group));
            object.set("superTopicSubs", properties.weibo_superTopic_subscribe.get(group));
            a += object + ",";
        }
        setting.setByGroup("subscribe", "weibo", (a.length() > 1 ? a.substring(0, a.length() - 1) : a) + "]");
        safeStoreConfig("微博订阅配置");
    }

    public void saveWeidianConfig() {
        String a = "[";
        for (long group : properties.weidian_cookie.keySet()) {
            JSONObject object = new JSONObject();
            object.set("qqGroup", group);
            object.set("cookie", properties.weidian_cookie.get(group).cookie);
            object.set("autoDeliver", properties.weidian_cookie.get(group).autoDeliver);
            object.set("highlight", properties.weidian_cookie.get(group).highlightItem.toString());
            a += object + ",";
        }
        setting.setByGroup("shops", "weidian", (a.length() > 1 ? a.substring(0, a.length() - 1) : a) + "]");
        safeStoreConfig("微店配置");
    }

    public boolean isAdmin(Group group, long qqID) {
        for (String g : properties.secureGroup) {
            if (g.equals(String.valueOf(group.getId())))
                return true;
        }

        for (String a : properties.admins) {
            if (a.equals(String.valueOf(qqID)))
                return true;
        }

        NormalMember m = group.get(qqID);
        if (m == null)
            return false;

        return m.getPermission() == MemberPermission.ADMINISTRATOR || m.getPermission() == MemberPermission.OWNER;
    }

    public boolean isAdmin(long qqID) {
        for (String a : properties.admins) {
            if (a.equals(String.valueOf(qqID)))
                return true;
        }
        return false;
    }
    
    // ========== 异步监控配置管理方法 ==========
    
    /**
     * 更新异步监控调度配置
     */
    public String updateAsyncMonitorSchedule(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return "❌ 无效的cron表达式格式";
        }
        
        String oldPattern = properties.async_monitor_schedule_pattern;
        setting.setByGroup("async_monitor", "schedule_pattern", cronExpression);
        safeStoreConfig("异步监控调度配置");
        properties.async_monitor_schedule_pattern = cronExpression;
        
        return String.format("✅ 异步监控调度已更新\n" +
                "旧配置: %s\n" +
                "新配置: %s", 
                oldPattern, cronExpression);
    }
    
    /**
     * 切换异步监控状态
     */
    public String switchAsyncMonitor() {
        boolean current = setting.getBool("async_monitor", "enable", true);
        setting.setByGroup("async_monitor", "enable", String.valueOf(!current));
        safeStoreConfig("异步监控配置");
        
        return !current ? "✅ 异步监控已启用" : "❌ 异步监控已禁用";
    }
    
    /**
     * 获取异步监控配置信息
     */
    public String getAsyncMonitorConfig() {
        String pattern = properties.async_monitor_schedule_pattern;
        
        return String.format("📊 异步监控配置信息\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "调度表达式: %s\n" +
                "其他配置项已迁移到monitor-config.properties",
                pattern);
    }

    /**
     * 保存异步监控订阅配置
     */
    public void saveAsyncMonitorSubscribeConfig() {
        // 检查setting是否已初始化，如果未初始化则尝试重新初始化
        if (setting == null) {
            Newboy.INSTANCE.getLogger().warning("ConfigOperator的setting为null，尝试重新初始化...");
            
            // 尝试重新获取Properties实例并初始化
            if (properties != null && properties.configData != null) {
                try {
                    this.setting = new Setting(properties.configData, StandardCharsets.UTF_8, false);
                    Newboy.INSTANCE.getLogger().info("ConfigOperator重新初始化成功");
                } catch (Exception e) {
                    Newboy.INSTANCE.getLogger().error("ConfigOperator重新初始化失败: " + e.getMessage(), e);
                    return;
                }
            } else {
                Newboy.INSTANCE.getLogger().error("ConfigOperator未正确初始化，properties或configData为null，无法保存异步监控订阅配置");
                return;
            }
        }
        
        AsyncOnlineStatusMonitor monitor = AsyncOnlineStatusMonitor.INSTANCE;
        List<SubscriptionConfig> subscriptions = monitor.getSubscriptionConfigs();
        
        // 将订阅配置列表转换为JSON格式: [{"qqGroup":253610309,"memberSubs":["胡丹"]}]
        StringBuilder subscribeJson = new StringBuilder("[");
        if (!subscriptions.isEmpty()) {
            for (int i = 0; i < subscriptions.size(); i++) {
                SubscriptionConfig config = subscriptions.get(i);
                subscribeJson.append("{\"qqGroup\":").append(config.getQqGroup()).append(",\"memberSubs\":[");
                
                Set<String> members = config.getMemberSubs();
                if (!members.isEmpty()) {
                    int memberIndex = 0;
                    for (String member : members) {
                        if (memberIndex > 0) subscribeJson.append(",");
                        subscribeJson.append("\"").append(member).append("\"");
                        memberIndex++;
                    }
                }
                subscribeJson.append("]}");
                
                if (i < subscriptions.size() - 1) {
                    subscribeJson.append(",");
                }
            }
        }
        subscribeJson.append("]");
        
        setting.setByGroup("subscribe", "async_monitor", subscribeJson.toString());
        safeStoreConfig("异步监控订阅配置");
    }
    
    /**
     * 加载异步监控订阅配置
     */
    public void loadAsyncMonitorSubscribeConfig() {
        // 检查setting是否已初始化
        if (setting == null) {
            Newboy.INSTANCE.getLogger().warning("ConfigOperator的setting为null，尝试重新初始化...");
            
            // 尝试重新获取Properties实例并初始化
            if (properties != null && properties.configData != null) {
                try {
                    this.setting = new Setting(properties.configData, StandardCharsets.UTF_8, false);
                    Newboy.INSTANCE.getLogger().info("ConfigOperator重新初始化成功");
                } catch (Exception e) {
                    Newboy.INSTANCE.getLogger().error("ConfigOperator重新初始化失败: " + e.getMessage(), e);
                    return;
                }
            } else {
                Newboy.INSTANCE.getLogger().error("ConfigOperator未正确初始化，properties或configData为null，无法加载异步监控订阅配置");
                return;
            }
        }
        
        String subscribeJson = setting.getStr("subscribe", "async_monitor", "[]");
        
        try {
            AsyncOnlineStatusMonitor monitor = AsyncOnlineStatusMonitor.INSTANCE;
            
            // 解析新格式的JSON配置: [{"qqGroup":253610309,"memberSubs":["胡丹"]}]
            if (subscribeJson != null && !subscribeJson.trim().equals("[]")) {
                String content = subscribeJson.trim();
                if (content.startsWith("[") && content.endsWith("]")) {
                    content = content.substring(1, content.length() - 1); // 移除外层方括号
                    
                    if (!content.trim().isEmpty()) {
                        // 简单解析JSON对象数组
                        String[] configs = content.split("\\},\\{");
                        for (String configStr : configs) {
                            configStr = configStr.trim();
                            if (!configStr.startsWith("{")) configStr = "{" + configStr;
                            if (!configStr.endsWith("}")) configStr = configStr + "}";
                            
                            // 解析单个配置对象
                            parseSubscriptionConfig(configStr, monitor);
                        }
                    }
                }
            }
            
            int totalMembers = monitor.getAllSubscribedMembers().size();
            int totalGroups = monitor.getSubscriptionConfigs().size();
            Newboy.INSTANCE.getLogger().info("异步监控订阅配置加载完成，共加载 " + totalGroups + " 个群组，" + totalMembers + " 个订阅成员");
            
        } catch (Exception e) {
            Newboy.INSTANCE.getLogger().error("加载异步监控订阅配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析单个订阅配置
     */
    private void parseSubscriptionConfig(String configStr, AsyncOnlineStatusMonitor monitor) {
        try {
            // 提取qqGroup
            int qqGroupStart = configStr.indexOf("\"qqGroup\":") + 10;
            int qqGroupEnd = configStr.indexOf(",", qqGroupStart);
            if (qqGroupEnd == -1) qqGroupEnd = configStr.indexOf("}", qqGroupStart);
            
            long qqGroup = Long.parseLong(configStr.substring(qqGroupStart, qqGroupEnd).trim());
            
            // 提取memberSubs数组
            int memberSubsStart = configStr.indexOf("\"memberSubs\":[") + 13;
            int memberSubsEnd = configStr.lastIndexOf("]");
            
            if (memberSubsStart < memberSubsEnd) {
                String membersStr = configStr.substring(memberSubsStart, memberSubsEnd);
                Set<String> memberSet = new HashSet<>();
                
                if (!membersStr.trim().isEmpty()) {
                    String[] memberArray = membersStr.split(",");
                    for (String member : memberArray) {
                        String cleanMember = member.trim().replaceAll("^\"|\"$", "");
                        if (!cleanMember.isEmpty()) {
                            memberSet.add(cleanMember);
                        }
                    }
                }
                
                // 添加到监控器
                monitor.addSubscriptionConfig(new SubscriptionConfig(qqGroup, memberSet));
            }
            
        } catch (Exception e) {
            Newboy.INSTANCE.getLogger().error("解析订阅配置失败: " + configStr + ", 错误: " + e.getMessage());
        }
    }
    
    // 旧的在线状态监控方法已移除，请使用异步监控系统
    
    /**
     * 安全保存配置，避免覆盖整个配置文件
     * 由于hutool Setting类的限制，暂时使用原有保存方式
     * 但添加了详细的日志记录以便用户了解配置变更
     * @param configType 配置类型描述
     */
    private void safeStoreConfig(String configType) {
        try {
            // 记录配置保存操作
            Newboy.INSTANCE.getLogger().info("正在保存" + configType + "...");
            
            // 使用原有的保存方式，但添加更好的错误处理
            setting.store();
            
            Newboy.INSTANCE.getLogger().info(configType + "已成功保存到: " + properties.configData.getAbsolutePath());
            
        } catch (Exception e) {
            Newboy.INSTANCE.getLogger().error("保存" + configType + "失败: " + e.getMessage(), e);
            throw e; // 重新抛出异常，让调用者知道保存失败
        }
    }
}
