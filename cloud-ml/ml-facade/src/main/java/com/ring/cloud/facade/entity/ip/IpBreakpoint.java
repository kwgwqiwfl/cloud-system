package com.ring.cloud.facade.entity.ip;

import com.ring.cloud.facade.common.ExecuteType;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//IP级断点（仅存当前IP的页数/计数）
@Data
public class IpBreakpoint {
    private int currentPage = 1;
    private int currentCount = 0;
    private ExecuteType executeType = ExecuteType.DEFAULT_IP;
    private Set<String> set = new HashSet<>();
    private List<Object> list = new ArrayList<>();

    private String key;//keyword 当前关键词
    private int level=0;//keyword 递归层级

    public void addCurrentCount(int num) { this.currentCount += num; }

    public void reset() {
        this.currentPage = 1;
        this.currentCount = 0;
        this.set.clear();
        this.list.clear();
        this.level = 0;
    }
}
