package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.MixDomainIp;
import com.ring.cloud.core.pojo.MixIpDomain;
import com.ring.welkin.common.persistence.service.BaseIdableService;
import java.util.List;

public interface MixDomainIpService extends BaseIdableService<Long, MixDomainIp> {
    void batchUpsert(List<MixDomainIp> list);

    PageResult<MixDomainIp> pageList(CommonPageQuery query);
}