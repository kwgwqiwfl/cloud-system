package com.ring.cloud.facade.entity.ip;

import lombok.Data;

/**
 * IP段实体类（存储段号、起始IP、结束IP、IP数量）
 */
@Data
public class IpSegment {
    private String segmentNo; // 段号（1~10）
    private String startIp; // 起始IP
    private String endIp=""; // 结束IP
//    private int firstPartStart; // 第一段起始值
//    private int firstPartEnd;   // 第一段结束值

    public IpSegment(String segmentNo, String startIp, String endIp) {
        this.segmentNo = segmentNo;
        this.startIp = startIp;
        this.endIp = endIp;
//        this.firstPartStart = firstPartStart;
//        this.firstPartEnd = firstPartEnd;
    }
    public IpSegment(String startIp) {
        this.startIp = startIp;
    }
    public IpSegment() {
    }

}
