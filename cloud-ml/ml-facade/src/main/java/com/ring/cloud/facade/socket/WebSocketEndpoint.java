package com.ring.cloud.facade.socket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
@ServerEndpoint("/ws")
public class WebSocketEndpoint {

    // 全局会话（线程安全）
    public static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.put(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable e) {
        // 忽略服务关闭时的线程终止异常，不打印错误日志
        if (e instanceof RejectedExecutionException ||
                (e.getMessage() != null && e.getMessage().contains("XNIO007007"))) {
            return;
        }
        log.error("[WebSocket] 异常", e);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 前端可发指令，无需处理
    }
}