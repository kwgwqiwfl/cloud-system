package com.ring.cloud.facade.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

//    @Value("${bloom.filter.expectedInsertions:100000000}")
//    private long expectedInsertions;
//
//    @Value("${bloom.filter.falseProbability:0.0001}")
//    private double falseProbability;
//
//    @Bean
//    public RBloomFilter<String> keywordBloomFilter(RedissonClient redissonClient) {
//        RBloomFilter<String> filter = redissonClient.getBloomFilter("keyword:bloom:filter");
//        // 安全初始化
//        if (!filter.isExists()) {
//            filter.tryInit(expectedInsertions, falseProbability);
//        }
//        return filter;
//    }
}