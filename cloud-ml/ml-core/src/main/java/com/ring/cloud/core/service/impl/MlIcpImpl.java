package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.MlIcpMapper;
import com.ring.cloud.core.pojo.MlIcp;
import com.ring.cloud.core.service.MlIcpService;
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
public class MlIcpImpl extends EntityClassServiceImpl<MlIcp> implements MlIcpService {

    @Autowired
    private MlIcpMapper mapper;

    @Override
    public MyIdableMapper<MlIcp> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<MlIcp> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<MlIcp> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }

    @Override
    public void updateInfo(MlIcp icp) {
        MlIcp exist = mapper.selectByDomain(icp.getDomain());

        if (exist == null) {
            mapper.insertSelective(icp);
            return;
        }

        if (DateUtil.isDifferentMonth(exist.getUpdateTime())) {
            exist.setQueryCount(icp.getQueryCount());
            mapper.updateByDomain(exist);
        }
    }

    @Override
    public void batchSaveOrUpdate(List<MlIcp> list) {
        Date today = DateUtil.today();
        for (MlIcp icp : list) {
            MlIcp exist = mapper.selectByDomain(icp.getDomain());
            if (exist == null) {
                icp.setCreateTime(today);
                icp.setUpdateTime(today);
                icp.setQueryCount(1);
                mapper.insertSelective(icp);
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