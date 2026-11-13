package com.zxyy.order.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.common.security.LoginSession;
import com.zxyy.order.config.WebSocketConfig;
import com.zxyy.order.domain.entity.ChatMessage;
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
    @Autowired
    private ChatService chatService;

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
    public void onMessage(Session session,String wsMessage) throws IOException {
        //消息先入库
        //获取管理员的id
        Authentication auth = (Authentication)session.getUserPrincipal();
        LoginSession principal = (LoginSession)auth.getPrincipal();
        Long adminId = principal.userId();
        // 当前登录的管理员ID
        //将消息转化为对应的实体
        WsMessage wsMes = objectMapper.readValue(wsMessage, WsMessage.class);
        ChatMessage chatMessage = null;
        //对不同类型的消息进行不同的处理
        switch(wsMes.type()){
            case "READ" -> {
                boolean isSuccess = chatService.adminReadMessage(wsMes);
                if(!isSuccess){
                    return;
                }
            }
            case "SEND_MESSAGE" -> {
                chatMessage = chatService.saveAdminMes(wsMes, adminId);
                //向发送者发送 ack数据库保存成功
                WsMessage wsMessage1 = new WsMessage("ACK",
                        wsMes.requestId(),wsMes.orderNo(),null, chatMessage.getSeq());
                String sendMes = objectMapper.writeValueAsString(wsMessage1);
                session.getBasicRemote().sendText(sendMes);
            }
            default -> throw new WsException("不确定的消息类型");
        }

        String orderNo = wsMes.orderNo();
        //查找订单号对应的session
        Set<Session> buyerSessions = chatConnectionManager.getBuyerSessions(orderNo);
        //将消息发送session
        switch (wsMes.type()){
            case "READ" ->{
                //通知管理员
                WsMessage push = new WsMessage(
                        "READ_RECEIPT",
                        wsMes.requestId(),
                        orderNo,
                        null,
                        Long.parseLong(wsMes.content())
                );
                String pushStr = objectMapper.writeValueAsString(push);
                buyerSessions.forEach(session1 -> {
                    try {
                        if(!session1.isOpen()){
                            return;
                        }
                        session1.getBasicRemote().sendText(pushStr);
                    } catch (IOException e) {
                        throw new WsException("通知管理员失败");
                    }
                });
            }
            case "SEND_MESSAGE"->{
                WsMessage push = new WsMessage(
                        "MESSAGE",
                        chatMessage.getClientMsgId(),
                        wsMes.orderNo(),
                        chatMessage.getContent(),
                        chatMessage.getSeq()
                );
                String s = objectMapper.writeValueAsString(push);
                buyerSessions.forEach(session1 -> {
                    try {
                        if(!session1.isOpen()){
                            return;
                        }
                        session1.getBasicRemote().sendText(s);
                    } catch (IOException e) {
                        throw new WsException("通知顾客失败");
                    }
                });
            }
            default -> throw new WsException("不支持的消息类型");
        }
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
