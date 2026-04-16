package com.ring.cloud.core.service;

import com.ring.cloud.core.entity.ip.IpDomainPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface IpDomainService extends BaseIdableService<Long, SourceIpDomain> {

    void joinDomains(String filePath);

    PageResult<SourceIpDomain> pageByIpNoCount(IpDomainPageQuery ipDomainPageQuery);

    int patchInsert(String tableName, String filePath) throws Exception;

}
