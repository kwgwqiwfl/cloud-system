package com.ring.cloud.core.service.impl;

import com.google.common.collect.Lists;
import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.entity.ip.IpDomainPageQuery;
import com.ring.cloud.core.frame.IpRouteInit;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.MixIpDomainMapper;
import com.ring.cloud.core.pojo.MixIpDomain;
import com.ring.cloud.core.service.MixIpDomainService;
import com.ring.cloud.core.util.IpCoreUtils;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class MixIpDomainServiceImpl extends EntityClassServiceImpl<MixIpDomain> implements MixIpDomainService {

    @Autowired
    private MixIpDomainMapper mapper;

    @Override
    public MyIdableMapper<MixIpDomain> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<MixIpDomain> pageList(CommonPageQuery query) {
        String ip = query.getKey().trim();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // ===================== 分表路由核心 =====================
        // 1. IP 转 long 计算表后缀 0~15
        long ipLong = IpCoreUtils.ipToLong(ip);
        int suffix = (int) (ipLong & Long.MAX_VALUE) % 16;
        String tableName = "mix_ip_" + suffix;

        // 分页偏移量
        int offset = (pageNum - 1) * pageSize;

        // 分页查询 + 总数
        List<MixIpDomain> list = mapper.selectPageByTable(tableName, ipLong, offset, pageSize);
//        long total = mapper.countIpByTable(tableName, ipLong);

        return PageResult.of(0, pageNum, pageSize, list);
    }

    @Override
    public PageResult<MixIpDomain> pageSpecifyList(CommonPageQuery query) {
        String ip = query.getKey().trim();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // 固定表名（不再分表）
        String tableName = "specify_ip_domain";

        // IP转long
        long ipLong = IpCoreUtils.ipToLong(ip);
        int offset = (pageNum - 1) * pageSize;

        List<MixIpDomain> list = mapper.selectPageByTable(tableName, ipLong, offset, pageSize);
        long total = mapper.countIpByTable(tableName, ipLong);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    @Override
    public PageResult<MixIpDomain> pageByIpNoCount(IpDomainPageQuery query) {
        String ip = query.getIp();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        String suffix = IpRouteInit.getTableSuffix(ip);
        String tableName = "mix_ip_" + suffix;

        int offset = (pageNum - 1) * pageSize;
        long ipLong = IpCoreUtils.ipToLong(ip);
        List<MixIpDomain> list = mapper.selectPageByTable(tableName, ipLong, offset, pageSize);

        return PageResult.of(0L, pageNum, pageSize, list);
    }

    // 根据IP查询全量列表
    public List<MixIpDomain> selectByIp(String ip) {
        String suffix = IpRouteInit.getTableSuffix(ip);
        String tableName = "mix_ip_" + suffix;
        return mapper.selectByIpDynamic(tableName, ip);
    }

    // ===================== 分表批量UPSERT（核心）=====================
    @Override
    public void batchUpsert(List<MixIpDomain> list) {
        // 按 IP 分表分组
        Map<Integer, List<MixIpDomain>> group = list.stream()
                .collect(Collectors.groupingBy(item -> {
                    long ipLong = item.getIpLong();
                    // Math.abs() 保证是正数 0~15
                    return Math.abs((int) (ipLong & Long.MAX_VALUE) % 16);
                }));

        // 分表分批写入
        group.forEach((index, data) -> {
            String tableName = "mix_ip_" + index;
            Lists.partition(data, 200).forEach(batch -> {
                mapper.batchUpsert(tableName, batch);
            });
        });
    }

    @Override
    public void specifyBatchUpsert(List<MixIpDomain> list) {
        // 固定表名
        String tableName = "specify_ip_domain";

        // 分批写入（每200条一批，保持原有逻辑）
        Lists.partition(list, 200).forEach(batch -> {
            mapper.batchUpsert(tableName, batch);
        });
    }
}