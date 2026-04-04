package com.ring.cloud.core.service;

import com.ring.cloud.core.pojo.IpRouteConfig;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface IpRouteService extends BaseIdableService<Long, IpRouteConfig> {

    List<IpRouteConfig> ipRouteList();
}
