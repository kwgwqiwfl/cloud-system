package com.ring.cloud.facade.controller;

import com.ring.cloud.core.entity.ip.CommonPageQuery;
import com.ring.cloud.facade.config.SpecifyIpSchedule;
import com.ring.cloud.facade.service.MixIpQueryService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/mixip")
@Api(tags = "mix ip查询接口")
public class MixIpQueryController {

    @Autowired
    private MixIpQueryService mixIpQueryService;

    @PostMapping("/pageIp")
    @ApiOperation(value = "IP分页查询")
    public MResponse<?> pageIp(@RequestBody @NotNull CommonPageQuery query) {
        try{
            return MResponse.ok(mixIpQueryService.pageIp(query));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/pageMixIpDomain")
    @ApiOperation(value = "IP域名分表分页查询")
    public MResponse<?> pageMixIpDomain(@RequestBody @NotNull CommonPageQuery query) {
        try{
            return MResponse.ok(mixIpQueryService.pageMixIpDomain(query));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/pageDomain")
    @ApiOperation(value = "域名分页查询")
    public MResponse<?> pageDomain(@RequestBody @NotNull CommonPageQuery query) {
        try{
            return MResponse.ok(mixIpQueryService.pageDomain(query));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/pageMixDomainIp")
    @ApiOperation(value = "域名IP关系分页查询")
    public MResponse<?> pageMixDomainIp(@RequestBody @NotNull CommonPageQuery query) {
        try {
            return MResponse.ok(mixIpQueryService.pageMixDomainIp(query));
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/pageIcp")
    @ApiOperation(value = "ICP备案分页查询")
    public MResponse<?> pageIcp(@RequestBody @NotNull CommonPageQuery query) {
        try{
            return MResponse.ok(mixIpQueryService.pageIcp(query));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/pageSubdomain")
    @ApiOperation(value = "子域名分页查询")
    public MResponse<?> pageSubdomain(@RequestBody @NotNull CommonPageQuery query) {
        try{
            return MResponse.ok(mixIpQueryService.pageSubdomain(query));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/pageSpecifyIpDomain")
    @ApiOperation(value = "指定ip列表域名分页查询")
    public MResponse<?> pageSpecifyIpDomain(@RequestBody @NotNull CommonPageQuery query) {
        try{
            return MResponse.ok(mixIpQueryService.pageSpecifyIpDomain(query));
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @ApiOperation(value = "动态添加指定IP到定时任务列表", notes = "GET 调用即可")
    @GetMapping("/specify/add/{ip}")
    public MResponse<?> addSpecifyIp(@PathVariable("ip") String ip) {
        SpecifyIpSchedule.addIp(ip);
        return MResponse.ok("IP添加成功：" + ip);
    }

}