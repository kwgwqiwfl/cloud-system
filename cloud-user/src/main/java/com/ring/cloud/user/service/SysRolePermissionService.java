package com.ring.cloud.user.service;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysRolePermission;
import com.ring.welkin.common.persistence.service.BaseIdableService;

public interface SysRolePermissionService extends BaseIdableService<Long, SysRolePermission> {
    PageResult<SysRolePermission> pageList(CommonPageQuery query);
}