package com.ring.cloud.user.controller;

import com.ring.cloud.common.frame.CommonPageQuery;
import com.ring.cloud.common.frame.PageResult;
import com.ring.cloud.user.entity.SysDept;
import com.ring.cloud.user.service.SysDeptService;
import com.ring.welkin.common.core.ml.MResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/sys/dept")
@Api(tags = "部门管理接口")
public class SysDeptController {

    @Autowired
    private SysDeptService sysDeptService;

    @PostMapping("/page")
    @ApiOperation(value = "部门分页查询")
    public MResponse<?> page(@RequestBody @NotNull CommonPageQuery query) {
        try {
            PageResult<SysDept> pageResult = sysDeptService.pageList(query);
            return MResponse.ok(pageResult);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info/{id}")
    @ApiOperation(value = "根据ID查询部门详情")
    public MResponse<?> getInfo(@PathVariable("id") Long id) {
        try {
            SysDept dept = sysDeptService.selectByPrimaryKey(id);
            return MResponse.ok(dept);
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/save")
    @ApiOperation(value = "新增部门")
    public MResponse<?> save(@RequestBody @NotNull SysDept sysDept) {
        try {
            sysDeptService.save(sysDept);
            return MResponse.ok("新增成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/update")
    @ApiOperation(value = "修改部门")
    public MResponse<?> update(@RequestBody @NotNull SysDept sysDept) {
        try {
            sysDeptService.update(sysDept);
            return MResponse.ok("修改成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/remove/{id}")
    @ApiOperation(value = "删除部门")
    public MResponse<?> remove(@PathVariable("id") Long id) {
        try {
            sysDeptService.deleteById(id);
            return MResponse.ok("删除成功");
        } catch (Throwable e) {
            return MResponse.error(400, e.getMessage());
        }
    }
}