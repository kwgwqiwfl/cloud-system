package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.DomainInout;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface DomainInoutService extends BaseIdableService<Long, DomainInout> {

    /**
     * 分页查询
     */
    PageResult<DomainInout> pageList(CommonPageQuery query);

    /**
     * 分页查询（不count）
     */
    PageResult<DomainInout> pageByInputDomainNoCount(CommonPageQuery query);

    /**
     * 批量UPSERT
     */
    void batchUpsert(List<DomainInout> list);

    /**
     * 批量导出汇总
     */
    void exportDomainData(List<String> inputDomainList, String exportDirPath);
}