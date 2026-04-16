package com.ring.cloud.facade.service;

import com.ring.cloud.core.entity.ip.IpDomainPageQuery;
import com.ring.cloud.core.frame.PageResult;
import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.cloud.core.service.IpDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IpDomainQueryService extends SeaCommon {

    @Autowired
    private IpDomainService ipDomainService;

    public void joinDomains(String filePath) {
        ipDomainService.joinDomains(filePath);
    }

    public PageResult<SourceIpDomain> pageByIpNoCount(IpDomainPageQuery ipDomainPageQuery) {
        return ipDomainService.pageByIpNoCount(ipDomainPageQuery);
    }
}
