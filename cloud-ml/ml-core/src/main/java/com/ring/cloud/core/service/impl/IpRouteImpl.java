package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.mybatis.mapper.IpRouteMapper;
import com.ring.cloud.core.pojo.IpRouteConfig;
import com.ring.cloud.core.service.IpRouteService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class IpRouteImpl extends EntityClassServiceImpl<IpRouteConfig> implements IpRouteService {
    @Autowired
    private IpRouteMapper mapper;

    @Override
    public MyIdableMapper<IpRouteConfig> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public List<IpRouteConfig> ipRouteList() {
        return mapper.selectAll();
    }
}
