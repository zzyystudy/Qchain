package com.zxyy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxyy.api.vo.order.ChatMessageVO;
import com.zxyy.order.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
            <script>
            SELECT id AS message_id, seq, client_msg_id, sender_id, content, create_time
            FROM chat_message
            WHERE conversation_id = #{conversationId}
              AND seq &lt;= #{lastSeq}
            <if test="beforeSeq != null">
                AND seq &lt; #{beforeSeq}
            </if>
            <if test="afterSeq != null">
                AND seq &gt; #{afterSeq}
            </if>
            ORDER BY seq
            <choose>
                <when test="afterSeq != null">ASC</when>
                <otherwise>DESC</otherwise>
            </choose>
            </script>
            """)
    IPage<ChatMessageVO> selectHistoryPage(Page<ChatMessageVO> page,
                                         @Param("conversationId") Long conversationId,
                                         @Param("lastSeq") Long lastSeq,
                                         @Param("beforeSeq") Long beforeSeq,
                                         @Param("afterSeq") Long afterSeq);
}
