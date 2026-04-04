package com.ring.cloud.facade.socket;

import com.alibaba.fastjson.JSON;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用websocket推送类
 * WsUtil.push(WsMessageType.LOG, "192.168.1.100 采集开始");
 */
public class WsUtil {

    /**
     * 通用推送：类型 + 任意内容
     */
    public static void push(WsMessageType type, Object content) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type.name().toLowerCase());
        msg.put("content", content);

        String json = JSON.toJSONString(msg);

        WebSocketEndpoint.SESSIONS.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(json);
                }
            } catch (Exception ignored) {}
        });
    }
}
