package com.zxyy.order.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.security.LoginSession;
import com.zxyy.order.config.WebSocketConfig;
import com.zxyy.order.exception.WsException;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@ServerEndpoint(
        value = "/ws/admin/chat",
        configurator = WebSocketConfig.class
)
public class AdminChatEndpoint {
    @Autowired
    private ChatConnectionManager chatConnectionManager;
    @Autowired
    private ObjectMapper objectMapper;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        //从session里面获取 用户id
        // 获取握手请求中已经认证的身份
        if (!(session.getUserPrincipal() instanceof Authentication auth)
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof LoginSession admin)) {

            session.close(new CloseReason(
                    CloseReason.CloseCodes.VIOLATED_POLICY,
                    "管理员身份无效"
            ));
            return;
        }
        // 当前登录的管理员ID
        Long adminId = admin.userId();
        chatConnectionManager.registerAdmin(adminId,session);
    }

    @OnMessage
    public void onMessage(Session session,String message) throws JsonProcessingException {
        //获取订单号
        WsMessage wsMessage = objectMapper.readValue(message, WsMessage.class);
        String orderNo = wsMessage.orderNo();
        //查找订单号对应的session
        Set<Session> buyerSessions = chatConnectionManager.getBuyerSessions(orderNo);
        //将消息发送session
        buyerSessions.forEach(session1 -> {
            try {
                if(!session1.isOpen()){
                    return;
                }
                session1.getBasicRemote().sendText(message);
            } catch (IOException e) {
                throw new WsException("通知顾客失败");
            }
        });
        //将消息存入数据库

    }
    @OnClose
    public void onClose(Session session){
        if (session.getUserPrincipal() instanceof Authentication auth
                && auth.getPrincipal() instanceof LoginSession admin) {

            chatConnectionManager.removeAdmin(admin.userId(), session);
        }

    }

    @OnError
    public void onError(Session session,Throwable error){
        if (session == null || !session.isOpen()) {
            return;
        }

        // 业务异常返回具体提示，其他异常返回统一提示
        String tip = error instanceof WsException
                ? error.getMessage()
                : "聊天服务异常，请稍后重试";

        session.getAsyncRemote().sendText(tip);
    }

}
