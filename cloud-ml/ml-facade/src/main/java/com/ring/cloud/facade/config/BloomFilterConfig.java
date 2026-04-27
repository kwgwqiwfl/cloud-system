package com.ring.cloud.facade.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Value("${bloom.filter.expectedInsertions:100000000}")
    private long expectedInsertions;

    @Value("${bloom.filter.falseProbability:0.0001}")
    private double falseProbability;

    @Bean
    public RBloomFilter<String> keywordBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> filter = redissonClient.getBloomFilter("keyword:bloom:filter");
        filter.tryInit(expectedInsertions, falseProbability);
        return filter;
    }
}