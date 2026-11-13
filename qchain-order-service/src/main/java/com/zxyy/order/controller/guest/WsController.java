package com.zxyy.order.controller.guest;

import com.zxyy.api.dto.order.GuestChatMessageQueryDTO;
import com.zxyy.api.vo.order.ChatHistoryVO;
import com.zxyy.common.result.Result;
import com.zxyy.order.service.ChatQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("guestChatController")
@RequestMapping("/guest/chat")
@Tag(name = "买家聊天查询")
public class WsController {
    @Autowired
    private ChatQueryService chatQueryService;

    @PostMapping("/messages/query")
    @Operation(summary = "验证订单号、预留邮箱和查询密码后查询聊天记录")
    public Result<ChatHistoryVO> getChatMessages(@Valid @RequestBody GuestChatMessageQueryDTO query) {
        return Result.success(chatQueryService.queryBuyerMessages(query));
    }
}
