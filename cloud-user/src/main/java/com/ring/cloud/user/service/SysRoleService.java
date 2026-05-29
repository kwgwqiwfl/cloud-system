package com.ring.cloud.user.service;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysRole;
import com.ring.welkin.common.persistence.service.BaseIdableService;

public interface SysRoleService extends BaseIdableService<Long, SysRole> {
    PageResult<SysRole> pageList(CommonPageQuery query);
}