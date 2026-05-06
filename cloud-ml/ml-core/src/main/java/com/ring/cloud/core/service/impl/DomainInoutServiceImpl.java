package com.ring.cloud.core.service.impl;

import com.google.common.collect.Lists;
import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.mybatis.mapper.DomainInoutMapper;
import com.ring.cloud.core.pojo.DomainInout;
import com.ring.cloud.core.service.DomainInoutService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DomainInoutServiceImpl extends EntityClassServiceImpl<DomainInout> implements DomainInoutService {

    @Autowired
    private DomainInoutMapper mapper;

    @Override
    public MyIdableMapper<DomainInout> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<DomainInout> pageList(CommonPageQuery query) {
        String inputDomain = query.getKey().trim();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // 分页偏移量
        int offset = (pageNum - 1) * pageSize;

        // 分页查询
        List<DomainInout> list = mapper.selectPageByInputDomain(inputDomain, offset, pageSize);

        return PageResult.of(0, pageNum, pageSize, list);
    }

    @Override
    public PageResult<DomainInout> pageByInputDomainNoCount(CommonPageQuery query) {
        return null;
    }

    @Override
    public void batchUpsert(List<DomainInout> list) {
        // 每200条一批插入，防止SQL过长
        Lists.partition(list, 200).forEach(batch -> {
            mapper.batchUpsert(batch);
        });
    }

}