package com.ring.cloud.facade.controller;

import com.ring.cloud.core.entity.ip.IpDomainPageQuery;
import com.ring.cloud.facade.service.IpDomainQueryService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/ipquery")
@Api(tags = "ip查询接口")
public class IpQueryController {

    @Autowired
    IpDomainQueryService ipQueryService;

    @PostMapping("/pageByIp")
    @ApiOperation(value = "启动单段操作")
    public MResponse<?> pageByIp(@RequestBody @NotNull IpDomainPageQuery ipDomainPageQuery) {
        try{
            return MResponse.ok(ipQueryService.pageByIp(ipDomainPageQuery));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
    @PostMapping("/pageByIpNoCount")
    @ApiOperation(value = "启动单段操作")
    public MResponse<?> pageByIpNoCount(@RequestBody @NotNull IpDomainPageQuery ipDomainPageQuery) {
        try{
            return MResponse.ok(ipQueryService.pageByIpNoCount(ipDomainPageQuery));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
//    @GetMapping("/batchIp/{startNo}")
//    @ApiOperation(value = "根据ip一段，自动启动后5段任务")
//    public MResponse<?> batchIp(@PathVariable("startNo") Integer startNo) {
//        ipService.batchIp(startNo);
//        return MResponse.ok("启动成功");
//    }

}
