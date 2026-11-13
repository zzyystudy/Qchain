package com.zxyy.order.ws;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxyy.order.domain.entity.ChatConversation;
import com.zxyy.order.domain.entity.ChatMessage;
import com.zxyy.order.service.ChatConversationService;
import com.zxyy.order.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatService {

    @Autowired
    private ChatConversationService chatConversationService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ObjectMapper objectMapper;


    @Transactional
    public ChatMessage saveBuyerMes(WsMessage wsMessage) throws JsonProcessingException {
        //1.判断对话表中是否有对应订单的记录
        String orderNo = wsMessage.orderNo();
        ChatConversation chatConversation = chatConversationService.lambdaQuery()
                .eq(ChatConversation::getOrderNo, orderNo)
                .one();
        //2.没有 就添加 然后再插入mes
        if(ObjectUtil.isEmpty(chatConversation)){
            chatConversation = ChatConversation.builder()
                    .orderNo(orderNo)
                    .lastSeq(0L)
                    .build();
            chatConversationService.save(chatConversation);
        }
        //3.有直接插入mes

        //先查数据库中有没有这条数据 有了直接返回
        ChatMessage one = chatMessageService.lambdaQuery()
                .eq(ChatMessage::getClientMsgId, wsMessage.requestId())
                .eq(ChatMessage::getSenderId,0L)
                .one();
        if(!ObjectUtil.isEmpty(one)){
            return one;
        }
        //3.1先更新conversion中的 lastSeq update会持有行锁到整个事务结束 所有后面可以先查询再提交
        chatConversationService.lambdaUpdate()
                .setSql("last_seq = last_seq + 1")
                .eq(ChatConversation::getOrderNo,orderNo)
                .update();
        //查询最新的last_seq
        chatConversation = chatConversationService.lambdaQuery()
                .eq(ChatConversation::getOrderNo, orderNo)
                .one();
        //构建 chatMessage
        ChatMessage chatMessage = ChatMessage.builder()
                .senderId(0L)
                .seq(chatConversation.getLastSeq())
                .clientMsgId(wsMessage.requestId())
                .conversationId(chatConversation.getId())
                .content(wsMessage.content())
                .build();
        chatMessageService.save(chatMessage);
        return chatMessage;
    }


    public boolean readMessage(WsMessage wsMes) {
        //更新这个chat_conversion中的客户端读取的最新消息
        long readSeq = Long.parseLong(wsMes.content());
        //判断是否大于数据库中的read
        return chatConversationService.lambdaUpdate()
                .set(ChatConversation::getBuyerReadSeq,readSeq)
                .eq(ChatConversation::getOrderNo,wsMes.orderNo())
                .lt(ChatConversation::getBuyerReadSeq,readSeq)
                .ge(ChatConversation::getLastSeq,readSeq)
                .update();
    }


    @Transactional
    public ChatMessage saveAdminMes(WsMessage wsMes, Long adminId) {
        //先查数据库中有没有这条数据 有了直接返回
        ChatMessage one = chatMessageService.lambdaQuery()
                .eq(ChatMessage::getClientMsgId, wsMes.requestId())
                .eq(ChatMessage::getSenderId,adminId)
                .one();
        if(!ObjectUtil.isEmpty(one)){
            return one;
        }
        //管理员不会主动发消息 所以一定有会话了
        //3.1先更新conversion中的 lastSeq update会持有行锁到整个事务结束 所有后面可以先查询再提交
        chatConversationService.lambdaUpdate()
                .setSql("last_seq = last_seq + 1")
                .eq(ChatConversation::getOrderNo,wsMes.orderNo())
                .update();
        //查询修改
        ChatConversation chatConversation = chatConversationService.lambdaQuery()
                .eq(ChatConversation::getOrderNo, wsMes.orderNo())
                .one();
        Long lastSeq = chatConversation.getLastSeq();

        //更新数据库
        ChatMessage chatMessage = ChatMessage.builder()
                .clientMsgId(wsMes.requestId())
                .content(wsMes.content())
                .conversationId(chatConversation.getId())
                .seq(lastSeq)
                .senderId(adminId)
                .build();
        chatMessageService.save(chatMessage);
        return chatMessage;
    }

    public boolean adminReadMessage(WsMessage wsMes) {
        //更新这个chat_conversion中的客户端读取的最新消息
        long readSeq = Long.parseLong(wsMes.content());
        return chatConversationService.lambdaUpdate()
                .set(ChatConversation::getAdminReadSeq,readSeq)
                .lt(ChatConversation::getAdminReadSeq,readSeq)
                .ge(ChatConversation::getLastSeq,readSeq)
                .eq(ChatConversation::getOrderNo,wsMes.orderNo())
                .update();
    }
}
