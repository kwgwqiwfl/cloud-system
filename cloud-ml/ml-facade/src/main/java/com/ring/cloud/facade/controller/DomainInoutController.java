package com.ring.cloud.facade.controller;

import com.ring.cloud.facade.service.DomainService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/domain")
@Api(tags = "域名输入输出IP管理接口")
public class DomainInoutController {

    @Autowired
    private DomainService domainService;

    // ======================== 导入 ========================
    @PostMapping("/import")
    @ApiOperation(value = "导入域名数据（支持csv、txt）")
    public MResponse<?> importDomain(@RequestParam("file") MultipartFile file) {
        try {
            int size = domainService.importDomainFile(file);
            return MResponse.ok("导入成功，有效域名个数："+size);
        } catch (Throwable e) {
            log.error("域名导入失败"+e.getMessage());
            return MResponse.error(400, "导入失败：" + e.getMessage());
        }
    }

    // ======================== 导出 ========================
    @PostMapping("/export")
    @ApiOperation(value = "导出域名数据（根据输入域名列表）分文件")
    public MResponse<?> exportDomain(@RequestBody List<String> inputDomainList) {
        try {
            return MResponse.ok("域名导出成功，文件目录："+domainService.exportDomainData(inputDomainList));
        } catch (Throwable e) {
            log.error("域名导出失败", e);
            return MResponse.error(400, "导出失败：" + e.getMessage());
        }
    }
    @GetMapping("/exportAll")
    @ApiOperation(value = "导出所有域名数据到一个文件")
    public MResponse<?> exportAllDomain() {
        try {
            return MResponse.ok("全量域名导出成功，文件："+domainService.exportAllDomainData());
        } catch (Throwable e) {
            log.error("全量域名导出失败", e);
            return MResponse.error(400, "导出失败：" + e.getMessage());
        }
    }
    // ======================== 导入 ========================
    @PostMapping("/test")
    @ApiOperation(value = "测试")
    public MResponse<?> test(@RequestBody List<String> inputDomainList) {
        try {
            domainService.test(inputDomainList);
            return MResponse.ok("导入成功");
        } catch (Throwable e) {
            log.error("域名导入失败", e);
            return MResponse.error(400, "导入失败：" + e.getMessage());
        }
    }

}