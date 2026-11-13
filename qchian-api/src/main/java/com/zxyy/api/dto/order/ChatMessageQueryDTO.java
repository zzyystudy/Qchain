package com.zxyy.api.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChatMessageQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 64)
    private String orderNo;

    // 翻历史：查询 seq 小于该值的消息
    @Min(1)
    private Long beforeSeq;

    // 补拉消息：查询 seq 大于该值的消息；0 表示从第一条开始
    @Min(0)
    private Long afterSeq;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer limit = 50;
}
