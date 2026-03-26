package com.ring.cloud.facade.config;

import com.ring.cloud.facade.entity.ip.IpSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
/**
 * IP地址段拆分工具类（0.0.0.0~255.255.255.255平均分成10段）
 */
@Slf4j
@Component
public class IpSegmentSplitConfig {

    // 预定义分段：前9段每段25个第一段值，第10段包含剩余所有
    private static final List<IpSegment> FIXED_SEGMENTS = new ArrayList<>();

//    static {
//        FIXED_SEGMENTS.add(new IpSegment(1, "0.0.0.1", "24.255.255.255", 0, 24));
//        FIXED_SEGMENTS.add(new IpSegment(2, "25.0.0.0", "49.255.255.255", 25, 49));
//        FIXED_SEGMENTS.add(new IpSegment(3, "50.0.0.0", "74.255.255.255", 50, 74));
//        FIXED_SEGMENTS.add(new IpSegment(4, "75.0.0.0", "99.255.255.255", 75, 99));
//        FIXED_SEGMENTS.add(new IpSegment(5, "100.0.0.0", "124.255.255.255", 100, 124));
//        FIXED_SEGMENTS.add(new IpSegment(6, "125.0.0.0", "149.255.255.255", 125, 149));
//        FIXED_SEGMENTS.add(new IpSegment(7, "150.0.0.0", "174.255.255.255", 150, 174));
//        FIXED_SEGMENTS.add(new IpSegment(8, "175.0.0.0", "199.255.255.255", 175, 199));
//        FIXED_SEGMENTS.add(new IpSegment(9, "200.0.0.0", "224.255.255.255", 200, 224));
//        FIXED_SEGMENTS.add(new IpSegment(10, "225.0.0.0", "255.255.255.255", 225, 255));
//    }

    static {
        FIXED_SEGMENTS.add(new IpSegment(1, "0.0.0.1", "2.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(2, "25.0.0.0", "26.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(3, "50.0.0.0", "51.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(4, "75.0.0.0", "76.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(5, "100.0.0.0", "101.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(6, "125.0.0.0", "126.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(7, "150.0.0.0", "151.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(8, "175.0.0.0", "176.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(9, "200.0.0.0", "201.255.255.255"));
        FIXED_SEGMENTS.add(new IpSegment(10, "225.0.0.0", "226.255.255.255"));
    }

//    static {
//////        FIXED_SEGMENTS.add(new IpSegment(2, "10.1.1.1", "10.1.1.255", 10, 10));
//        FIXED_SEGMENTS.add(new IpSegment(1, "3.1.1.3", "3.1.1.247", 3, 3));
//        FIXED_SEGMENTS.add(new IpSegment(2, "4.1.1.1", "4.1.1.9", 4, 4));
//    }

    // 核心：快速判断IP所属段
    public IpSegment getSegmentByIp(String ip) {
        //解析IP第一段数值
        int firstPart;
        try {
            firstPart = Integer.parseInt(ip.split("\\.")[0]);
        } catch (Throwable e) {
            throw new IllegalArgumentException("无效IP地址：" + ip);
        }

        //直接匹配所属段
        if (firstPart >= 0 && firstPart <= 24) return FIXED_SEGMENTS.get(0);
        if (firstPart >= 25 && firstPart <= 49) return FIXED_SEGMENTS.get(1);
        if (firstPart >= 50 && firstPart <= 74) return FIXED_SEGMENTS.get(2);
        if (firstPart >= 75 && firstPart <= 99) return FIXED_SEGMENTS.get(3);
        if (firstPart >= 100 && firstPart <= 124) return FIXED_SEGMENTS.get(4);
        if (firstPart >= 125 && firstPart <= 149) return FIXED_SEGMENTS.get(5);
        if (firstPart >= 150 && firstPart <= 174) return FIXED_SEGMENTS.get(6);
        if (firstPart >= 175 && firstPart <= 199) return FIXED_SEGMENTS.get(7);
        if (firstPart >= 200 && firstPart <= 224) return FIXED_SEGMENTS.get(8);
        if (firstPart >= 225 && firstPart <= 255) return FIXED_SEGMENTS.get(9);

        throw new IllegalArgumentException("IP第一段数值超出范围：" + firstPart);
    }

    // 扩展：仅返回段号（如果只需要段号，效率更高）
    public int getSegmentNoByIp(String ip) {
        int firstPart = Integer.parseInt(ip.split("\\.")[0]);
        if (firstPart <= 24) return 1;
        if (firstPart <= 49) return 2;
        if (firstPart <= 74) return 3;
        if (firstPart <= 99) return 4;
        if (firstPart <= 124) return 5;
        if (firstPart <= 149) return 6;
        if (firstPart <= 174) return 7;
        if (firstPart <= 199) return 8;
        if (firstPart <= 224) return 9;
        return 10;
    }

    // 直接返回预定义分段，零计算、最高效
    public List<IpSegment> splitIpSegments() {
        return FIXED_SEGMENTS;
    }
}
