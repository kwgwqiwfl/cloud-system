package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.MlSubdomainMapper;
import com.ring.cloud.core.pojo.MlSubdomain;
import com.ring.cloud.core.service.MlSubdomainService;
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
public class MlSubdomainImpl extends EntityClassServiceImpl<MlSubdomain> implements MlSubdomainService {

    @Autowired
    private MlSubdomainMapper mapper;

    @Override
    public MyIdableMapper<MlSubdomain> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<MlSubdomain> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<MlSubdomain> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }

    @Override
    public void updateInfo(MlSubdomain subdomain) {
        MlSubdomain exist = mapper.selectByDomain(subdomain.getDomain());

        if (exist == null) {
            mapper.insertSelective(subdomain);
            return;
        }

        if (DateUtil.isDifferentMonth(exist.getUpdateTime())) {
            exist.setQueryCount(subdomain.getQueryCount());
            mapper.updateByDomain(exist);
        }
    }

    @Override
    public void batchSaveOrUpdate(List<MlSubdomain> list) {
        mapper.batchSaveOrUpdate(list);
//        Date today = DateUtil.today();
//        for (MlSubdomain sub : list) {
//            MlSubdomain exist = mapper.selectByDomain(sub.getDomain());
//            if (exist == null) {
//                sub.setCreateTime(today);
//                sub.setUpdateTime(today);
//                sub.setQueryCount(1);
//                mapper.insertSelective(sub);
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