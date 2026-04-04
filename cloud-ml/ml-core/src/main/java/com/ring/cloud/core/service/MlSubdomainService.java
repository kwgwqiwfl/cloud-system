package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.MlSubdomain;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface MlSubdomainService extends BaseIdableService<Long, MlSubdomain> {

    /**
     * 分页查询（按子域名key模糊/精确查询）
     */
    PageResult<MlSubdomain> pageList(CommonPageQuery query);

    /**
     * 对外暴露更新接口
     */
    void updateInfo(MlSubdomain mlSubdomain);

    void batchSaveOrUpdate(List<MlSubdomain> list);
}