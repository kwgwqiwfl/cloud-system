package com.ring.cloud.facade.service;

import com.ring.cloud.core.service.IpDomainService;
import com.ring.cloud.facade.crawl.ProxyUtil;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.entity.proxy.ProxyResponse;
import com.ring.cloud.facade.execute.TaskHandlerExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
public abstract class HandleCommon {
    @Autowired
    protected RestTemplate restTemplate;
    @Value("${ml.client.proxy.url:null}")
    private String proxyUrl;

    public List<ProxyIp> getProxy() {
        if(proxyUrl.length()<5)
            throw new IllegalArgumentException("代理地址配置不正确");
        ProxyResponse pr = restTemplate.getForObject(proxyUrl, ProxyResponse.class);
        if(pr==null || pr.getCode()!=200 || pr.getData()==null || pr.getData().getProxy_list()==null || pr.getData().getProxy_list().size()<1)
            throw new IllegalArgumentException("代理查询结果异常");
        return ProxyUtil.parseIp(pr);
    }
//    //批量插入映射关系
//    public void patchAddDraftShineList(List<DraftShine> shines) {
//        if(ICollections.hasElements(shines))
//            ListUtil.subgroupList(shines).forEach(list -> draftShineService.insertList(list));
//    }
//    //批量添加节点属性
//    public void patchAddNodePropList(List<DraftNodeProp> newProps) {
//        if(ICollections.hasElements(newProps))
//            ListUtil.subgroupList(newProps).forEach(list -> draftNodePropService.insertList(list));
//    }
//    //批量添加节点字段
//    public void patchAddNodeFieldList(List<DraftNodeField> fields) {
//        if(ICollections.hasElements(fields))
//            ListUtil.subgroupList(fields).forEach(list -> draftNodeFieldService.insertList(list));
//    }
//    //批量添加节点列表
//    public void patchAddNodeInfoList(List<DraftNodeInfo> infos) {
//        if(ICollections.hasElements(infos))
//            ListUtil.subgroupList(infos).forEach(list -> draftNodeInfoService.insertList(list));
//    }
}
