package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.MlDomain;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface MlDomainService extends BaseIdableService<Long, MlDomain> {

    /**
     * 分页查询（按域名key模糊/精确查询）
     */
    PageResult<MlDomain> pageList(CommonPageQuery query);

    /**
     * 对外暴露更新接口
     */
    void updateInfo(MlDomain mlDomain);

    void batchSaveOrUpdate(List<MlDomain> list);
}