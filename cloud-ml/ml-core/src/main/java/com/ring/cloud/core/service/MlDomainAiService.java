package com.ring.cloud.core.service;

import com.ring.cloud.core.pojo.MlDomainAi;
import com.ring.welkin.common.persistence.service.BaseIdableService;

import java.util.List;

public interface MlDomainAiService extends BaseIdableService<Long, MlDomainAi> {

    void batchSaveOrUpdate(List<MlDomainAi> list);
}