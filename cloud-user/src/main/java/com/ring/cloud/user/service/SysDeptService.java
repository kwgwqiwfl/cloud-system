package com.ring.cloud.user.service;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysDept;
import com.ring.welkin.common.persistence.service.BaseIdableService;

public interface SysDeptService extends BaseIdableService<Long, SysDept> {
    PageResult<SysDept> pageList(CommonPageQuery query);
}