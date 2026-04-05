package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.MlDomainMapper;
import com.ring.cloud.core.pojo.MlDomain;
import com.ring.cloud.core.service.MlDomainService;
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
public class MlDomainImpl extends EntityClassServiceImpl<MlDomain> implements MlDomainService {

    @Autowired
    private MlDomainMapper mapper;

    @Override
    public MyIdableMapper<MlDomain> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<MlDomain> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<MlDomain> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }

    @Override
    public void updateInfo(MlDomain domain) {
        MlDomain exist = mapper.selectByDomain(domain.getDomain());

        if (exist == null) {
            mapper.insertSelective(domain);
            return;
        }

        if (DateUtil.isDifferentMonth(exist.getUpdateTime())) {
            exist.setQueryCount(domain.getQueryCount());
            mapper.updateByDomain(exist);
        }
    }
    @Override
    public void batchSaveOrUpdate(List<MlDomain> list) {
        mapper.batchSaveOrUpdate(list);
//        Date today = DateUtil.today();
//        for (MlDomain domain : list) {
//            MlDomain exist = mapper.selectByDomain(domain.getDomain());
//            if (exist == null) {
//                domain.setCreateTime(today);
//                domain.setUpdateTime(today);
//                domain.setQueryCount(1);
//                mapper.insertSelective(domain);
//            } else {
//                if (DateUtil.isDifferentMonth(exist.getUpdateTime())) {
//                    exist.setQueryCount(exist.getQueryCount() + 1);
//                }
//                exist.setUpdateTime(today);
//                mapper.updateByPrimaryKeySelective(exist);
//            }
//        }
    }
}