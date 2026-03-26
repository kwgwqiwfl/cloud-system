package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.mybatis.mapper.IpDomainMapper;
import com.ring.cloud.core.pojo.SourceIpDomain;
import com.ring.cloud.core.service.IpDomainService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import com.ring.welkin.common.queryapi.query.example.ExampleQuery;
import com.ring.welkin.common.queryapi.query.field.FieldGroup;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IpDomainImpl extends EntityClassServiceImpl<SourceIpDomain> implements IpDomainService {
    @Autowired
    private IpDomainMapper mapper;

    @Override
    public MyIdableMapper<SourceIpDomain> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public List<SourceIpDomain> byIp(String ip) {
        return selectList(ExampleQuery.builder(getEntityClass()).andEqualTo("ip", ip));
    }

    @Override
    public void deleteByIp(String ip) {
        FieldGroup fieldGroup = FieldGroup.builder();
        if (StringUtils.isNotEmpty(ip))
            fieldGroup.andEqualTo("ip", ip);
        delete(ExampleQuery.builder().fieldGroup(fieldGroup));
    }
}
