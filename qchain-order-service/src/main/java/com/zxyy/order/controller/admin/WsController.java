package com.zxyy.order.controller.admin;

import com.zxyy.api.dto.order.ChatConversationPageQueryDTO;
import com.zxyy.api.dto.order.ChatMessageQueryDTO;
import com.zxyy.api.vo.order.ChatHistoryVO;
import com.zxyy.common.result.PageResult;
import com.zxyy.common.result.Result;
import com.zxyy.order.service.ChatQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminChatController")
@RequestMapping("/admin/chat")
@Tag(name = "管理端聊天查询")
public class WsController {
    @Autowired
    private ChatQueryService chatQueryService;

    @GetMapping("/conversations")
    @Operation(summary = "分页查询订单会话及客服未读数")
    public Result<PageResult> getConversations(@Valid @ModelAttribute ChatConversationPageQueryDTO query) {
        return Result.success(chatQueryService.pageConversations(query));
    }

    @GetMapping("/messages")
    @Operation(summary = "查询订单聊天记录，支持向前翻页和断线补拉")
    public Result<ChatHistoryVO> getMessages(@Valid @ModelAttribute ChatMessageQueryDTO query) {
        return Result.success(chatQueryService.queryMessages(query));
    }
}
