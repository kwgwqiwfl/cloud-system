package com.ring.cloud.user.service.impl;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysRolePermission;
import com.ring.cloud.user.mybatis.mapper.SysRolePermissionMapper;
import com.ring.cloud.user.service.SysRolePermissionService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SysRolePermissionServiceImpl extends EntityClassServiceImpl<SysRolePermission> implements SysRolePermissionService {

    @Autowired
    private SysRolePermissionMapper mapper;

    @Override
    public MyIdableMapper<SysRolePermission> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public PageResult<SysRolePermission> pageList(CommonPageQuery query) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<SysRolePermission> list = mapper.pageList(query.getKey(), offset, query.getPageSize());
        return PageResult.of(0L, query.getPageNum(), query.getPageSize(), list);
    }
}