package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.MlIpMapper;
import com.ring.cloud.core.pojo.MlIp;
import com.ring.cloud.core.service.MlIpService;
import com.ring.cloud.core.util.DateUtil;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class MlIpImpl extends EntityClassServiceImpl<MlIp> implements MlIpService {

    @Autowired
    private MlIpMapper mapper;

    @Override
    public MyIdableMapper<MlIp> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<MlIp> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<MlIp> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }

    @Override
    public void updateInfo(MlIp ip) {
        MlIp exist = mapper.selectByIp(ip.getIp());

        if (exist == null) {
            mapper.insertSelective(ip);
            return;
        }

        if (DateUtil.isDifferentMonth(exist.getUpdateTime())) {
            exist.setQueryCount(ip.getQueryCount());
            mapper.updateByIp(exist);
        }
    }

    @Override
    public void batchSaveOrUpdate(List<MlIp> list) {
        Date today = DateUtil.today();
        for (MlIp ip : list) {
            MlIp exist = mapper.selectByIp(ip.getIp());
            if (exist == null) {
                ip.setCreateTime(today);
                ip.setUpdateTime(today);
                ip.setQueryCount(1);
                mapper.insertSelective(ip);
            } else {
                if (DateUtil.isDifferentMonth(exist.getUpdateTime())) {
                    exist.setQueryCount(exist.getQueryCount() + 1);
                }
                exist.setUpdateTime(today);
                mapper.updateByPrimaryKeySelective(exist);
            }
        }
    }
}