package com.ring.cloud.user.service.impl;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysPermission;
import com.ring.cloud.user.mybatis.mapper.SysPermissionMapper;
import com.ring.cloud.user.service.SysPermissionService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SysPermissionServiceImpl extends EntityClassServiceImpl<SysPermission> implements SysPermissionService {

    @Autowired
    private SysPermissionMapper mapper;

    @Override
    public MyIdableMapper<SysPermission> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<SysPermission> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<SysPermission> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }
}