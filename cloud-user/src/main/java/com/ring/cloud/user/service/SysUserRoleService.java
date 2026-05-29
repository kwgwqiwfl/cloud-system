package com.ring.cloud.user.service;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysUserRole;
import com.ring.welkin.common.persistence.service.BaseIdableService;

public interface SysUserRoleService extends BaseIdableService<Long, SysUserRole> {
    PageResult<SysUserRole> pageList(CommonPageQuery query);
}