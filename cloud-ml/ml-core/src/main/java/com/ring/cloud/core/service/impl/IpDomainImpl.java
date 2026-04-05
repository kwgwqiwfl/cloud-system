package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.entity.ip.IpDomainPageQuery;
import com.ring.cloud.core.frame.IpRouteInit;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.IpDomainMapper;
import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.cloud.core.service.IpDomainService;
import com.ring.cloud.core.util.DateUtil;
import com.ring.cloud.core.util.IpCoreUtils;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class IpDomainImpl extends EntityClassServiceImpl<SourceIpDomain> implements IpDomainService {
    @Autowired
    private IpDomainMapper mapper;

    @Override
    public MyIdableMapper<SourceIpDomain> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<SourceIpDomain> pageByIp(IpDomainPageQuery query) {
        String ip = query.getIp();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // ====================== 路由核心 ======================
        String suffix = IpRouteInit.getTableSuffix(ip);
        String tableName = "ip_domain_" + suffix;

        // 计算 offset
        int offset = (pageNum - 1) * pageSize;
        long ipLong = IpCoreUtils.ipToLong(ip);
        // 查询数据
        List<SourceIpDomain> list = mapper.selectPageByTable(tableName, ipLong, offset, pageSize);
        long total = mapper.countIpByTable(tableName, ipLong);

        return PageResult.of(total, pageNum, pageSize, list);
    }

    @Override
    public PageResult<SourceIpDomain> pageByIpNoCount(IpDomainPageQuery query) {
        String ip = query.getIp();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // ====================== 路由核心 ======================
        String suffix = IpRouteInit.getTableSuffix(ip);
        String tableName = "ip_domain_" + suffix;

        // 计算 offset
        int offset = (pageNum - 1) * pageSize;
        long ipLong = IpCoreUtils.ipToLong(ip);
        // 查询数据
        List<SourceIpDomain> list = mapper.selectPageByTable(tableName, ipLong, offset, pageSize);

        return PageResult.of(0L, pageNum, pageSize, list);
    }

//    @Override
//    public List<SourceIpDomain> byIp(String ip) {
//        // 你已经写好的 → 获取分表名
//        String tableName = getTargetTableName(ip);
//
//        // 直接调用注解SQL，最高效率
//        return mapper.selectByIpDynamic(tableName, ip);
////        return selectList(ExampleQuery.builder(getEntityClass()).andEqualTo("ip", ip));
//    }

    /**
     * 根据IP获取目标分表表名
     * 规则：
     * 1. 先查路由表（你后面加）
     * 2. 没查到 → 小IP自动分配 17/18/19/20
     */
    private String getTargetTableName(String ip) {

        // ================= 小IP自动规则 =================
        // 取IP第一段
        String[] arr = ip.split("\\.");
        String first = arr[0];
        int num = Integer.parseInt(first);

        // 自动分配到 17 18 19 20
        int suffix = (num % 4) + 17;

        // 返回表名：ip_domain_17、ip_domain_18...
        return "ip_domain_" + suffix;
    }


    @Override
    public int patchInsert(String tableName, String filePath) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {

            List<SourceIpDomain> batch = new ArrayList<>(DateUtil.BATCH_SIZE);
            String line;
            int total = 0;

            br.readLine(); // 跳过表头

            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", -1);

                SourceIpDomain bean = new SourceIpDomain();
//                bean.setId(Long.valueOf(cols[0].trim())); // 读文件里的id
                bean.setIp(cols[1].trim());
                bean.setLoc(cols[2].trim());
                bean.setDomain(cols[3].trim());
                bean.setAdtime(DateUtil.parseDate(cols[4]));
                bean.setUptime(DateUtil.parseDate(cols[5]));

                batch.add(bean);

                if (batch.size() >= DateUtil.BATCH_SIZE) {
                    total += mapper.batchInsert(tableName, batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                total += mapper.batchInsert(tableName, batch);
            }

            return total;
        }
    }

    public int loadCsvToTmpTable(String tableName, String filePath) {
        return mapper.loadCsvToTmpTable(tableName, filePath);
    }
}
