package com.ring.cloud.facade.process.metrics;

import com.ring.cloud.facade.config.IpCrawlExecutor;
import com.ring.cloud.facade.config.PangIpServcie;
import com.ring.cloud.facade.config.ProxyPoolManager;
import com.ring.cloud.facade.frame.RestProxyBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
//@Transactional
public abstract class TaskOperate<T> implements ITaskManage<T> {
//    @Autowired
//    protected FeignWaiterClient feignWaiterClient;
//    @Autowired
//    protected RestTemplate restTemplate;
    @Autowired
    protected RestProxyBase restProxyBase;
    @Autowired
    protected IpCrawlExecutor ipCrawlExecutor;
//    @Autowired
//    protected ProxyApiClient proxyClient;
    @Autowired
    protected PangIpServcie pangIpServcie;
    @Autowired
    protected ProxyPoolManager proxyPoolManager;


}
