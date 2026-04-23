package com.ring.cloud.core.service.impl;

import com.ring.cloud.core.mybatis.mapper.MlDomainAiMapper;
import com.ring.cloud.core.pojo.MlDomainAi;
import com.ring.cloud.core.service.MlDomainAiService;
import com.ring.welkin.common.persistence.mybatis.mapper.MyIdableMapper;
import com.ring.welkin.common.persistence.service.entity.EntityClassServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MlDomainAiImpl extends EntityClassServiceImpl<MlDomainAi> implements MlDomainAiService {

    @Autowired
    private MlDomainAiMapper mapper;

    @Override
    public MyIdableMapper<MlDomainAi> getMyIdableMapper() {
        return mapper;
    }

    @Override
    public void batchSaveOrUpdate(List<MlDomainAi> list) {
        mapper.batchSaveOrUpdate(list);
    }
}