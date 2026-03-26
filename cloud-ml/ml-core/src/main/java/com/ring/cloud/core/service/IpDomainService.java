package com.ring.cloud.core.service;

import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface IpDomainService extends BaseIdableService<Long, SourceIpDomain> {

    List<SourceIpDomain> byIp(String ip);

    void deleteByIp(String rankType);

}
