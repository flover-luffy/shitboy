package net.luffy.handler;

import cn.hutool.http.HttpRequest;
import net.luffy.Newboy;
import net.luffy.model.*;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.InputStream;

public class WeidianSenderHandler {

    public WeidianSenderHandler() {
    }

    public InputStream getRes(String resLoc) {
        return HttpRequest.get(resLoc).execute().bodyStream();
    }

    //普链订单播报, pickAmount = 5
    public WeidianItemMessage executeItemMessages(WeidianItem item, Group group) {
        return this.executeItemMessages(item, group, 5);
    }

    public WeidianItemMessage executeItemMessages(WeidianItem item, Group group, int pickAmount) {
        WeidianHandler weidian = Newboy.INSTANCE.getHandlerWeidian();
        WeidianCookie cookie = Newboy.INSTANCE.getProperties().weidian_cookie.get(group.getId());

        //统计总值
        long id = item.id;
        WeidianBuyer[] buyers = weidian.getItemBuyer(cookie, id);

        Image image = null;
        if (!item.pic.equals("")) {
            try (ExternalResource imageResource = ExternalResource.create(getRes(item.pic))) {
                image = group.uploadImage(imageResource);
            } catch (Exception e) {
                // 忽略图片上传失败
            }
        }

        return WeidianItemMessage.construct(item.id, item.name, image, buyers, pickAmount);
    }

    public WeidianOrderMessage executeOrderMessage(WeidianOrder order, Group group) {
        return WeidianOrderMessage.construct(order);
    }

}