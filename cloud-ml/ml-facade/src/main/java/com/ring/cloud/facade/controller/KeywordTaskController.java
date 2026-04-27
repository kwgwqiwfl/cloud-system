package com.ring.cloud.facade.controller;

import com.ring.cloud.facade.service.KeywordService;
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
@RequestMapping("/keyword")
@Api(tags = "关键词下拉采集接口")
public class KeywordTaskController {

    @Autowired
    KeywordService keywordService;

    // ====================== 1. GET 无参方法 ======================
    @GetMapping("/start")
    @ApiOperation(value = "启动关键词下拉采集任务")
    public MResponse<?> startTask() {
        try {
            keywordService.startKeywordTask();
            return MResponse.ok("任务启动成功");
        } catch (Throwable e) {
            log.error("关键词任务启动失败" + e.getMessage());
            return MResponse.error(400, "启动失败：" + e.getMessage());
        }
    }

    // ====================== 2. 导入文件方法（完全对齐你给的格式） ======================
    @PostMapping("/import")
    @ApiOperation(value = "导入关键词数据（支持csv、txt）")
    public MResponse<?> importKeyword(
            @RequestParam("file") MultipartFile file) {
        try {
            int size = keywordService.importKeywordFile(file);
            return MResponse.ok("导入成功，有效关键词个数：" + size);
        } catch (Throwable e) {
            log.error("关键词导入失败" + e.getMessage());
            return MResponse.error(400, "导入失败：" + e.getMessage());
        }
    }

    // ======================== 导入 ========================
    @PostMapping("/test")
    @ApiOperation(value = "测试")
    public MResponse<?> test(@RequestBody List<String> list) {
        try {
            keywordService.test(list);
            return MResponse.ok("导入成功");
        } catch (Throwable e) {
            log.error("域名导入失败", e);
            return MResponse.error(400, "导入失败：" + e.getMessage());
        }
    }
}