package com.zxyy.common.config;

import org.redisson.spring.cache.CacheConfig;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTtlCacheConfig extends CacheConfig {

    private final long jitterMillis;

    public RandomTtlCacheConfig(Duration baseTtl, Duration jitter) {
        // 基础 TTL、最大空闲时间
        super(baseTtl.toMillis(), 0);

        this.jitterMillis = jitter.toMillis();

        if (super.getTTL() <= 0 || jitterMillis < 0) {
            throw new IllegalArgumentException(
                    "基础 TTL 必须大于 0，随机范围不能为负数"
            );
        }

        // 提前检查毫秒相加是否会溢出
        Math.addExact(super.getTTL(), jitterMillis);
    }

    @Override
    public long getTTL() {
        // 每次调用都重新生成随机偏移
        long randomMillis = ThreadLocalRandom.current()
                .nextLong(jitterMillis + 1);

        return super.getTTL() + randomMillis;
    }
}
