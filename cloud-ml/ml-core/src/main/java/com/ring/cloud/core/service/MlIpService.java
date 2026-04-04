package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.MlIp;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface MlIpService extends BaseIdableService<Long, MlIp> {

    /**
     * 分页查询（按IP key模糊/精确查询）
     */
    PageResult<MlIp> pageList(CommonPageQuery query);

    /**
     * 对外暴露更新接口
     */
    void updateInfo(MlIp mlIp);

    void batchSaveOrUpdate(List<MlIp> list);
}