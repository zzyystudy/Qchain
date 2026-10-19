package com.zxyy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxyy.order.domain.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMessage> {
}
