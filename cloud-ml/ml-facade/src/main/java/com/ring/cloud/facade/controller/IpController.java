package com.ring.cloud.facade.controller;

import com.ring.cloud.facade.entity.IpDomainRequest;
import com.ring.cloud.facade.entity.ip.IpSegment;
import com.ring.cloud.facade.service.IpService;
import com.ring.welkin.common.core.result.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ip")
@Api(tags = "ip源信息接口")
public class IpController {

    @Autowired
    IpService ipService;

    @PostMapping("/startSingleIp")
    @ApiOperation(value = "单段操作")
    public Response<?> startSingleIp(@RequestBody @NotNull IpSegment ipSegment) {
        try{
            ipService.startSingleIp(ipSegment);
            return Response.ok("启动成功");
        }catch (Throwable e) {
            return Response.ok(e.getMessage());
        }
    }

    @GetMapping("/stopSingleIp/{segNo}")
    @ApiOperation(value = "停止单个任务")
    public Response<?> stopSingleIp(@PathVariable("segNo") Integer segNo) {
        ipService.stopSingleIp(segNo);
        return Response.ok("启动成功");
    }

    @GetMapping("/batchIp/{startNo}")
    @ApiOperation(value = "停止单个任务")
    public Response<?> batchIp(@PathVariable("startNo") Integer startNo) {
        ipService.batchIp(startNo);
        return Response.ok("启动成功");
    }

    @GetMapping("/runningSeg")
    @ApiOperation(value = "一步操作")
    public Response<?> runningSeg() {
        return Response.ok(ipService.runningSeg());
    }
    @GetMapping("/crawlTest")
    @ApiOperation(value = "一步操作")
    public Response<?> crawlTest() {
        ipService.startIpTask(null);
        return Response.ok().build();
    }
    @GetMapping("/test")
    @ApiOperation(value = "仅测试")
    public Response<?> crawlTest1() {
        log.info("abc");
        return Response.ok().build();
    }

    @PostMapping("/startIpTask")
    @ApiOperation(value = "启动ip-domain任务")
    public Response<?> startIpTask(@RequestBody @NotNull IpDomainRequest ipDomainRequest) {
        ipService.startIpTask(ipDomainRequest);
        return Response.ok().build();
    }

    @PostMapping("/crawlByFactors")
    @ApiOperation(value = "获取ip列表")
    public Response<?> crawlByFactors(@RequestBody @NotNull List<String> rankList) {
        return Response.ok().build();
    }

}
