package com.ring.cloud.facade.controller;

import com.ring.cloud.core.pojo.IpRouteConfig;
import com.ring.cloud.facade.entity.ip.IpImport;
import com.ring.cloud.facade.entity.ip.IpSegment;
import com.ring.cloud.facade.service.IpService;
import com.ring.welkin.common.core.ml.MResponse;
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
public class IpTaskController {

    @Autowired
    IpService ipService;

    @PostMapping("/startSingleIpList")
    @ApiOperation(value = "启动list中所有ip单个任务")
    public MResponse<?> startSingleIpList(@RequestBody @NotNull List<String> ipList) {
        try{
            ipService.startSingleIpList(ipList);
            return MResponse.ok("启动成功");
        }catch (Throwable e) {
            return MResponse.ok(e.getMessage());
        }
    }

    @PostMapping("/startSegIp")
    @ApiOperation(value = "根据开始ip 启动单段操作")
    public MResponse<?> startSegIp(@RequestBody @NotNull IpSegment ipSegment) {
        try{
            ipService.startSegIp(ipSegment);
            return MResponse.ok("启动成功");
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
    @PostMapping("/startIpList")
    @ApiOperation(value = "启动单段，自动补全结束ip")
    public MResponse<?> startIpList(@RequestBody @NotNull List<String> startIpList) {
        try{
            ipService.startIpList(startIpList);
            return MResponse.ok("启动成功");
        }catch (Throwable e) {
            return MResponse.ok(e.getMessage());
        }
    }
    @GetMapping("/batchIp/{startNo}")
    @ApiOperation(value = "根据ip第一段序号，自动启动后5段任务")
    public MResponse<?> batchIp(@PathVariable("startNo") Integer startNo) {
        ipService.batchIp(startNo);
        return MResponse.ok("启动成功");
    }

    @GetMapping("/startLargeIp/{targetIp}")
    @ApiOperation(value = "开启大ip分段任务")
    public MResponse<?> startLargeIp(@PathVariable("targetIp") String targetIp) {
        ipService.startSingleIpAutoSplit(targetIp);
        return MResponse.ok("启动成功");
    }

    @GetMapping("/stopTask/{key}")
    @ApiOperation(value = "停止单个任务")
    public MResponse<?> stopTask(@PathVariable("key") String key) {
        return MResponse.ok(key+ipService.stopTask(key));
    }

    @GetMapping("/runningSeg")
    @ApiOperation(value = "一步操作")
    public MResponse<?> runningSeg() {
        return MResponse.ok(ipService.runningSeg());
    }

    @GetMapping("/progress/{targetIp}")
    @ApiOperation(value = "查询单个IP采集总进度")
    public MResponse<?> getProgress(@PathVariable("targetIp") String targetIp) {
        return MResponse.ok(ipService.getIpProgress(targetIp));
    }

    @GetMapping("/test")
    @ApiOperation(value = "仅测试")
    public MResponse<?> crawlTest1() {
        log.info("abc");
        return MResponse.ok();
    }

    @PostMapping("/crawlByFactors")
    @ApiOperation(value = "获取ip列表")
    public MResponse<?> crawlByFactors(@RequestBody @NotNull List<String> rankList) {
        return MResponse.ok();
    }

    @PostMapping("/patchInsertTmp")
    @ApiOperation(value = "导入文件到临时表")
    public MResponse<?> patchInsertTmp(@RequestBody @NotNull IpImport ipImport) {
        try{
            return MResponse.ok(ipService.patchInsertTmp(ipImport));
        }catch (Throwable e) {
            return MResponse.ok(e.getMessage());
        }
    }

    @PostMapping("/syncIpRoute")
    @ApiOperation(value = "同步ip路由")
    public MResponse<?> syncIpRoute(@RequestBody @NotNull IpRouteConfig ipRouteConfig) {
        try{
            ipService.syncIpRoute(ipRouteConfig);
            return MResponse.ok("添加成功");
        }catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
}
