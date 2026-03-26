package com.ring.cloud.facade.config;

import com.ring.cloud.facade.service.IpService;
import com.ring.welkin.common.core.Initializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 初始化任务
 */
@Slf4j
@Component
public class IpTaskInitializer implements Initializer {

    @Autowired
    private IpService ipService;

    @Override
    public void init() {
        log.info("初始化开始");
    }

//    @Transactional(rollbackOn = Exception.class)
//    synchronized void initTaskDir() {
////        Tenant defaultTenant = tenantService.findByName("default");
////        User defaultUser = userService.findByTenantAndUsername(defaultTenant.getId(), "admin");
////        resourceSupportService.createTaskResourceDesc(defaultTenant.getId(), defaultUser);
//    }

}
