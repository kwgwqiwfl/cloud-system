package com.ring.cloud.user.service.impl;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysDept;
import com.ring.cloud.user.mybatis.mapper.SysDeptMapper;
import com.ring.cloud.user.service.SysDeptService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SysDeptServiceImpl extends EntityClassServiceImpl<SysDept> implements SysDeptService {

    @Autowired
    private SysDeptMapper mapper;

    @Override
    public MyIdableMapper<SysDept> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<SysDept> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<SysDept> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }
}