package com.zxyy.order.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.order.config.WebSocketConfig;
import com.zxyy.order.domain.entity.ChatMessage;
import com.zxyy.order.exception.WsException;
import com.zxyy.order.mapper.ShopOrderMapper;
import com.zxyy.order.util.PasswordUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ServerEndpoint(
        value = "/ws/chat/{orderNo}",
        configurator = WebSocketConfig.class
)
public class BuyerChatEndpoint {
    @Autowired
    private ShopOrderMapper shopOrderMapper;
    @Autowired
    private ChatConnectionManager chatConnectionManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ChatService chatService;

    @OnOpen
    public void onOpen(Session session, @PathParam("orderNo") String orderNo) throws IOException {
        //从session中获取密码
        var values =
                session.getRequestParameterMap().get("password");
        String password = values == null || values.isEmpty() ? null : values.get(0);
        //校验订单密码
        String passwordHash = shopOrderMapper.selectPasswordHashByOrderNo(orderNo);
        boolean matches = PasswordUtil.matches(password, passwordHash);
        if(!matches){
            //校验失败直接关闭连接 释放资源
            session.close(new CloseReason(
                    CloseReason.CloseCodes.VIOLATED_POLICY,
                    "密码错误"
            ));
            return;
        }
        //校验成功 注册连接
        chatConnectionManager.registerBuyer(orderNo,session);
    }

    @OnMessage
    public void onMessage(Session session,String wsMessage,@PathParam("orderNo") String orderNo) throws IOException {
        //先进行消息持久化
        //这个方法不加事务注解 事务将会失效
        WsMessage wsMes = objectMapper.readValue(wsMessage, WsMessage.class);
        //校验订单号
        if(!wsMes.orderNo().equals(orderNo)){
            throw new WsException("订单与当前连接不同");
        }

        //1.先将数据保存到数据库中 然后再说推送的事
        //对不同类型的消息进行不同的处理 这里先处理入库和ack的返回
        ChatMessage chatMessage = null;
        switch(wsMes.type()){
            case "READ" -> {
                boolean isSuccess = chatService.readMessage(wsMes);
                if(!isSuccess){
                    return;
                }
            }
            case "SEND_MESSAGE" -> {
                chatMessage = chatService.saveBuyerMes(wsMes);
                //向发送者发送 ack数据库保存成功
                WsMessage wsMessage1 = new WsMessage("ACK",
                        wsMes.requestId(),wsMes.orderNo(),null, chatMessage.getSeq());
                String sendMes = objectMapper.writeValueAsString(wsMessage1);
                session.getBasicRemote().sendText(sendMes);
            }
            default -> throw new WsException("不确定的消息类型");
        }

        //先查一下这个订单有没有对应的管理员
        Long adminId = chatConnectionManager.selectAdminByOrderNo(orderNo);
        //如果消息类型是read 当前没有人接待就直接返回 不需要随机找管理员推送
        if(adminId == null && "READ".equals(wsMes.type())){
            return;
        }

        if(adminId == null){
            //说明没有绑定过管理员 绑定管理员
            //先获取一下在线的管理员
            Set<Long> connectedAdminIds = chatConnectionManager.getConnectedAdminIds();
            //然后通过负载均衡 分发给某个管理员 接待
            List<Long> list = new ArrayList<>(connectedAdminIds);
            //这里判断这个集合是不是为空 管理员都不在线
            if(list.isEmpty()){
                throw new WsException("管理员不在线，已留言");
            }
            adminId = list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }


        //绑定管理端 获取管理端session
        boolean binded = chatConnectionManager.bindOrderAdmin(orderNo, adminId);
        if(!binded){
            throw new WsException("连接管理端失败");
        }
        Set<Session> adminSessions = chatConnectionManager.getAdminSessions(adminId);
        //将消息传递给管理端 应该是每个都要发
        //2. 这里获取到在线的管理员之后应该再次switch 进行消息的转发
        switch (wsMes.type()){
            case "READ" -> {
                WsMessage push = new WsMessage(
                        "READ_RECEIPT",
                        wsMes.requestId(),
                        orderNo,
                        null,
                        Long.parseLong(wsMes.content())
                );
                String s = objectMapper.writeValueAsString(push);
                adminSessions.forEach(session1 -> {
                    try {
                        if(!session1.isOpen()){
                            return;
                        }
                        session1.getBasicRemote().sendText(s);
                    } catch (IOException e) {
                        throw new WsException("通知管理员失败");
                    }
                });
            }
            case "SEND_MESSAGE" ->{
                WsMessage push = new WsMessage(
                        "MESSAGE",
                        chatMessage.getClientMsgId(),
                        wsMes.orderNo(),
                        chatMessage.getContent(),
                        chatMessage.getSeq()
                );
                String s = objectMapper.writeValueAsString(push);
                adminSessions.forEach(session1 -> {
                    try {
                        if(!session1.isOpen()){
                            return;
                        }
                        session1.getBasicRemote().sendText(s);
                    } catch (IOException e) {
                        throw new WsException("通知管理员失败");
                    }
                });
            }
            default -> throw new WsException("不支持的消息类型");
        }

    }

    @OnClose
    public void onClose(Session session,@PathParam("orderNo") String orderNo){
        chatConnectionManager.removeBuyer(orderNo,session);
    }

    @OnError
    public void onError(Session session,Throwable error){
        // 这里可以记录异常日志

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
