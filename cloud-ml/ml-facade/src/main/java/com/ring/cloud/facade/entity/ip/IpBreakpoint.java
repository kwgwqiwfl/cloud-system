package com.ring.cloud.facade.entity.ip;

import lombok.Data;

//IP级断点（仅存当前IP的页数/计数）
@Data
public class IpBreakpoint {
    private int currentPage = 1;
    private int currentCount = 0;

    public void addCurrentCount(int num) { this.currentCount += num; }

    public void reset() {
        this.currentPage = 1;
        this.currentCount = 0;
    }
}
