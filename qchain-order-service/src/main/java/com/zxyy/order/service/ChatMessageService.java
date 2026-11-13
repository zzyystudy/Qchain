package com.zxyy.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxyy.order.domain.entity.ChatMessage;
import com.zxyy.order.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {
}
