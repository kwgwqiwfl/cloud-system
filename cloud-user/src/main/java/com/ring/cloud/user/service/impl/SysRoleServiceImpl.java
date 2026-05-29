package com.ring.cloud.user.service.impl;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysRole;
import com.ring.cloud.user.mybatis.mapper.SysRoleMapper;
import com.ring.cloud.user.service.SysRoleService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SysRoleServiceImpl extends EntityClassServiceImpl<SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMapper mapper;

    @Override
    public MyIdableMapper<SysRole> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<SysRole> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<SysRole> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }
}