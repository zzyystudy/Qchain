package com.zxyy.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxyy.api.dto.order.ChatConversationPageQueryDTO;
import com.zxyy.api.dto.order.ChatMessageQueryDTO;
import com.zxyy.api.dto.order.GuestChatMessageQueryDTO;
import com.zxyy.api.vo.order.ChatConversationVO;
import com.zxyy.api.vo.order.ChatHistoryVO;
import com.zxyy.api.vo.order.ChatMessageVO;
import com.zxyy.common.exception.BaseException;
import com.zxyy.common.result.PageResult;
import com.zxyy.common.util.HmacSha256Util;
import com.zxyy.order.domain.entity.ChatConversation;
import com.zxyy.order.domain.entity.ShopOrder;
import com.zxyy.order.exception.PasswordError;
import com.zxyy.order.mapper.ChatConversationMapper;
import com.zxyy.order.mapper.ChatMessageMapper;
import com.zxyy.order.mapper.ShopOrderMapper;
import com.zxyy.order.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ChatQueryService {
    @Autowired
    private ChatConversationMapper conversationMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private ShopOrderMapper orderMapper;

    public PageResult pageConversations(ChatConversationPageQueryDTO query) {
        var page = conversationMapper.selectConversationPage(
                new Page<ChatConversationVO>(query.getPage(), query.getPageSize()), query.getOrderNo());
        return new PageResult(page.getTotal(), page.getRecords());
    }

    public ChatHistoryVO queryMessages(ChatMessageQueryDTO query) {
        if (query.getBeforeSeq() != null && query.getAfterSeq() != null) {
            throw new BaseException("beforeSeq 和 afterSeq 不能同时使用");
        }

        ChatConversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getOrderNo, query.getOrderNo()));
        if (conversation == null) {
            return ChatHistoryVO.builder()
                    .orderNo(query.getOrderNo()).lastSeq(0L).buyerReadSeq(0L).adminReadSeq(0L)
                    .messages(List.of()).hasMore(false).build();
        }

        // 多取一条判断 hasMore，实际 LIMIT 由已有的 MP 分页插件生成。
        var page = messageMapper.selectHistoryPage(
                new Page<ChatMessageVO>(1, query.getLimit() + 1L, false),
                conversation.getId(), conversation.getLastSeq(), query.getBeforeSeq(), query.getAfterSeq());
        List<ChatMessageVO> messages = new ArrayList<>(page.getRecords());
        boolean hasMore = messages.size() > query.getLimit();
        if (hasMore) {
            messages.remove(messages.size() - 1);
        }
        if (query.getAfterSeq() == null) {
            Collections.reverse(messages);
        }

        return ChatHistoryVO.builder()
                .orderNo(conversation.getOrderNo()).lastSeq(conversation.getLastSeq())
                .buyerReadSeq(conversation.getBuyerReadSeq()).adminReadSeq(conversation.getAdminReadSeq())
                .messages(messages).hasMore(hasMore).build();
    }

    public ChatHistoryVO queryBuyerMessages(GuestChatMessageQueryDTO query) {
        // 每次查询都验证该订单的凭据；不要求已支付，也不调用商品或交付接口。
        ShopOrder order = orderMapper.selectOne(new LambdaQueryWrapper<ShopOrder>()
                .select(ShopOrder::getId, ShopOrder::getQueryPasswordHash, ShopOrder::getBuyerEmailFingerprint)
                .eq(ShopOrder::getOrderNo, query.getOrderNo()));
        if (order == null) {
            throw new PasswordError("订单号、邮箱或查询密码不正确");
        }
        boolean passwordMatches = PasswordUtil.matches(query.getPassword(), order.getQueryPasswordHash());
        boolean emailMatches = HmacSha256Util.verify(
                query.getEmail().toLowerCase(Locale.ROOT), order.getBuyerEmailFingerprint());
        if (!passwordMatches || !emailMatches) {
            throw new PasswordError("订单号、邮箱或查询密码不正确");
        }
        return queryMessages(query);
    }
}
