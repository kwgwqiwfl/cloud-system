package com.ring.cloud.core.service.impl;

import com.google.common.collect.Lists;
import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.MixDomainIpMapper;
import com.ring.cloud.core.pojo.MixDomainIp;
import com.ring.cloud.core.service.MixDomainIpService;
import com.ring.cloud.core.util.IpCoreUtils;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class MixDomainIpServiceImpl extends EntityClassServiceImpl<MixDomainIp> implements MixDomainIpService {

    @Resource
    private MixDomainIpMapper mapper;

    @Override
    public MyIdableMapper<MixDomainIp> getMyIdableMapper() {
        return mapper;
    }

    /**
     * 域名分表批量写入（和 IP 分表规则完全统一）
     * 分片依据：domain_crc % 16
     */
    @Override
    public void batchUpsert(List<MixDomainIp> list) {
        Map<Integer, List<MixDomainIp>> group = list.stream()
                .collect(Collectors.groupingBy(item -> {
                    int crc = item.getDomainCrc();
                    // ✅ 最终固定：最简单、最安全、永不出负数
                    return Math.abs(crc % 16);
                }));

        group.forEach((index, data) -> {
            String tableName = "mix_domain_" + index;
            Lists.partition(data, 200).forEach(batch -> {
                mapper.batchUpsert(tableName, batch);
            });
        });
    }

    @Override
    public PageResult<MixDomainIp> pageList(CommonPageQuery query) {
        String domain = query.getKey().trim();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // 1. 域名转 crc（查询用）
        Integer domainCrc = IpCoreUtils.crc32(domain);

        // 2. 分页偏移量
        int offset = (pageNum - 1) * pageSize;

        // 3. 根据 domainCrc 查询（单表，不分表）
        List<MixDomainIp> list = mapper.selectPageByDomainCrc(domainCrc, offset, pageSize);

        // 4. 不查总数
        return PageResult.of(0, pageNum, pageSize, list);
    }
}