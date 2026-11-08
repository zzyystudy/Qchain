package com.zxyy.order.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.order.config.WebSocketConfig;
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
    public void onMessage(Session session,String message,@PathParam("orderNo") String orderNo){
        //先查一下这个订单有没有对应的管理员
        Long adminId = chatConnectionManager.selectAdminByOrderNo(orderNo);

        if(adminId == null){
            //说明没有绑定过管理员 绑定管理员
            //先获取一下在线的管理员
            Set<Long> connectedAdminIds = chatConnectionManager.getConnectedAdminIds();
            //然后通过负载均衡 分发给某个管理员 接待
            List<Long> list = new ArrayList<>(connectedAdminIds);
            //这里判断这个集合是不是为空 管理员都不在线
            if(list.isEmpty()){
                throw new WsException("管理员不在线，已留言"); //TODO 将消息 保存到数据库
            }
            adminId = list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }


        //绑定管理端 获取管理端session
        boolean binded = chatConnectionManager.bindOrderAdmin(orderNo, adminId);
        if(!binded){
            throw new WsException("连接管理端失败");
        }
        Set<Session> adminSessions = chatConnectionManager.getAdminSessions(adminId);
        //将消息传递给管理端 应该是每个都要发 TODO 消息存入数据库
        adminSessions.forEach(session1 -> {
            try {
                if(!session1.isOpen()){
                    return;
                }
                WsMessage wsMessage = new WsMessage(orderNo, message);
                String toMessage = objectMapper.writeValueAsString(wsMessage);
                session1.getBasicRemote().sendText(toMessage);
            } catch (IOException e) {
                throw new WsException("通知管理员失败");
            }
        });
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
