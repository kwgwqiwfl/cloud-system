package com.ring.cloud.facade.controller;

import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import com.ring.cloud.facade.service.IpApiService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "ip api接口")
public class IpApiController {

    @Autowired
    IpApiService ipApiService;

    @GetMapping("/queryDomainByIp/{ip}/{endAddTime}")
    @ApiOperation(value = "根据ip查询域名")
    public MResponse<?> queryDomainByIp(@PathVariable("ip") String ip, @PathVariable("endAddTime") String endAddTime) {
        try {
            String fileName = ipApiService.queryDomainByIp(ip, endAddTime);
            return MResponse.ok("查询成功生成文件："+fileName);
        } catch (Throwable e) {
            log.error("ip反查域名失败" + e.getMessage());
            return MResponse.error(400, "反查域名失败：" + e.getMessage());
        }

    }

    @GetMapping("/ipdata/{ip}")
    @ApiOperation(value = "根据ip查询归属地")
    public MResponse<?> ipdata(@PathVariable("ip") String ip) {
        try {
            String fileName = ipApiService.ipdata(ip);
            return MResponse.ok("查询成功生成文件："+fileName);
        } catch (Throwable e) {
            log.error("ip查询归属地失败" + e.getMessage());
            return MResponse.error(400, "查询归属地失败：" + e.getMessage());
        }
    }
    @Autowired
    protected GlobalProxyHelper globalProxyHelper;
    @GetMapping("/testProxy")
    @ApiOperation(value = "仅测试")
    public MResponse<?> crawlTest1() {
        log.info("abc");
        globalProxyHelper.getAvailableProxy();
        return MResponse.ok();
    }
}
