package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.entity.ip.IpDomainPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.IpDomainMapper;
import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.cloud.core.service.IpDomainService;
import com.ring.cloud.core.util.DateUtil;
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
    public void joinDomains(String filePath) {
        mapper.exportToFileByDomainTempTable(filePath);
    }

    @Override
    public PageResult<SourceIpDomain> pageByIpNoCount(IpDomainPageQuery query) {
//        String ip = query.getIp();
//        int pageNum = query.getPageNum();
//        int pageSize = query.getPageSize();
//
//        // ====================== 路由核心 ======================
//        String suffix = IpRouteInit.getTableSuffix(ip);
//        String tableName = "ip_domain_" + suffix;
//
//        // 计算 offset
//        int offset = (pageNum - 1) * pageSize;
//        long ipLong = IpCoreUtils.ipToLong(ip);
//        // 查询数据
//        List<SourceIpDomain> list = mapper.selectPageByTable(tableName, ipLong, offset, pageSize);
//
//        return PageResult.of(0L, pageNum, pageSize, list);
        return PageResult.of(0L, 1, 1, new ArrayList<>());
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
