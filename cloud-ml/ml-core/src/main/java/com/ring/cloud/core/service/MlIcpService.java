package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.MlIcp;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface MlIcpService extends BaseIdableService<Long, MlIcp> {

    /**
     * 分页查询（按备案域名key模糊/精确查询）
     */
    PageResult<MlIcp> pageList(CommonPageQuery query);

    /**
     * 对外暴露更新接口
     */
    void updateInfo(MlIcp mlIcp);

    void batchSaveOrUpdate(List<MlIcp> list);
}