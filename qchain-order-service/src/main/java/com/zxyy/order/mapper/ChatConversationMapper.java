package com.zxyy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxyy.api.vo.order.ChatConversationVO;
import com.zxyy.order.domain.entity.ChatConversation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    @Select("""
            <script>
            SELECT c.id AS conversation_id, c.order_no, c.last_seq,
                   c.buyer_read_seq, c.admin_read_seq,
                   m.content AS last_message, m.create_time AS last_message_time,
                   (SELECT COUNT(*) FROM chat_message unread
                    WHERE unread.conversation_id = c.id
                      AND unread.sender_id = 0
                      AND unread.seq &gt; c.admin_read_seq
                      AND unread.seq &lt;= c.last_seq) AS unread_count
            FROM chat_conversation c
            LEFT JOIN chat_message m
                   ON m.conversation_id = c.id AND m.seq = c.last_seq
            WHERE c.last_seq &gt; 0
            <if test="orderNo != null and orderNo != ''">
                AND c.order_no = #{orderNo}
            </if>
            ORDER BY m.create_time DESC, c.id DESC
            </script>
            """)
    IPage<ChatConversationVO> selectConversationPage(Page<ChatConversationVO> page,
                                                   @Param("orderNo") String orderNo);
}
