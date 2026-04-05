package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.entity.ip.IpDomainPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.MixIpDomain;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface MixIpDomainService extends BaseIdableService<Long, MixIpDomain> {

    /**
     * 根据IP分页查询
     */
    PageResult<MixIpDomain> pageList(CommonPageQuery query);

    PageResult<MixIpDomain> pageSpecifyList(CommonPageQuery query);

    /**
     * 根据IP分页查询（不count）
     */
    PageResult<MixIpDomain> pageByIpNoCount(IpDomainPageQuery ipDomainPageQuery);

    /**
     * 批量UPSERT插入（分表专用）
     */
    void batchUpsert(List<MixIpDomain> list);

    /**
     * 批量UPSERT插入（Specify专用）
     */
    void specifyBatchUpsert(List<MixIpDomain> list);

}