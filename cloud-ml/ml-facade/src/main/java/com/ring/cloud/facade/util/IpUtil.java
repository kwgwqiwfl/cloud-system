package com.ring.cloud.facade.util;

import com.ring.cloud.facade.entity.ip.IpSegment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * ip工具类
 */
@Slf4j
public class IpUtil {
    // ThreadLocal：每个线程独享一个数组，避免多线程数据覆盖 对比ip使用
    private static final ThreadLocal<int[]> IP_PARTS_THREAD_LOCAL = ThreadLocal.withInitial(() -> new int[4]);
    // 单例Random（避免重复创建，提升效率）
    private static final Random RANDOM = new Random();

    /**
     * 生成50~100（包含50和100）的随机整数
     * 用于分页请求睡眠间隔
     */
    public static int getRandom10To30() {
        // 公式：最小值 + nextInt(最大值-最小值+1) → 10 + nextInt(21)（0~20 → 10~30）
        return 10 + RANDOM.nextInt(21);
    }
    //生成ip domain url 第一页查询 url="https://site.ip138.com/1.1.1.1";
    public static String buildIpUrlFirst(String ip, String ipDomainUrl) {
        return ipDomainUrl + "/" + ip;
    }

    //生成ip domain url 分页查询 url="https://site.ip138.com/index/querybyip/?ip=1.1.1.1&page=2&token=2ea08c94ef895a05b7df3182717f8dc2";
    public static String buildIpUrlPage(int page, String ip, String ipDomainUrl, String ipPageInfix, String dynamicToken) {
        return ipDomainUrl + "/" + ipPageInfix + "/?ip=" + ip + "&page=" + page + "&token=" + dynamicToken;
    }

    //生成ip pangzhan url="https://chapangzhan.com/3.1.1.0/24";
    public static String buildPangUrl(String ip, String ipPangUrl) {
        return ipPangUrl + "/" + ip + "/24";
    }

    /**
     * 校验字符串是否包含【全部】子串
     * @param str 原字符串（可为null/空）
     * @param searchStrs 待校验的子串数组（可为null/空）
     * @return true=包含所有子串；false=缺少任意一个/原字符串为空/子串数组为空
     */
    public static boolean validStrContains(CharSequence str, CharSequence... searchStrs) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        // 遍历校验是否包含所有子串
        for (CharSequence searchStr : searchStrs) {
            if (!StringUtils.contains(str, searchStr)) {
                return false;
            }
        }
        return true;
    }

    public static List<String> parsePangValidIps(String xmlContent) {
        if(!validStrContains(xmlContent, "c-bd", "tfoot"))//校验html字符串
            throw new IllegalArgumentException("pang xml异常");
        List<String> ips = new ArrayList<>();
            Document doc = Jsoup.parse(xmlContent);
            Element table = doc.selectFirst("div.c-bd table");
            assert table != null;
            Elements trList = table.select("tbody tr.J_link");
            for (Element tr : trList) {
                Elements tdList = tr.select("td");
                if(CollectionUtils.isEmpty(tdList))
                    throw new IllegalArgumentException("pang 列表异常");
                ips.add(tdList.get(0).text());
            }
            return ips;
    }

    // 获取本段起始IP
    public static String getSegmentStartIp(String ip) {
        String[] p = ip.split("\\.");
        return p[0] + "." + p[1] + "." + p[2] + ".0";
    }
    // 判断 ip1 < ip2
    public static boolean ipLessThan(String ip1, String ip2) {
        String[] p1 = ip1.split("\\.");
        String[] p2 = ip2.split("\\.");
        for (int i = 0; i < 4; i++) {
            long n1 = Long.parseLong(p1[i]);
            long n2 = Long.parseLong(p2[i]);
            if (n1 < n2) return true;
            if (n1 > n2) return false;
        }
        return false;
    }
    /**
     * ip1 是否 大于 ip2
     * @return true = ip1 > ip2
     */
    public static boolean ipGreaterThan(String ip1, String ip2) {
        String[] p1 = ip1.split("\\.");
        String[] p2 = ip2.split("\\.");
        for (int i = 0; i < 4; i++) {
            long n1 = Long.parseLong(p1[i]);
            long n2 = Long.parseLong(p2[i]);
            if (n1 > n2) return true;
            if (n1 < n2) return false;
        }
        return false;
    }

    //下一个ip段 不管是否越界
    public static String nextSegmentIp(String ip) {
        String[] p = ip.split("\\.");
        int p1 = Integer.parseInt(p[0]);
        int p2 = Integer.parseInt(p[1]);
        int p3 = Integer.parseInt(p[2]);

        p3++;
        if (p3 > 255) {
            p3 = 0;
            p2++;
            if (p2 > 255) {
                p2 = 0;
                p1++;
            }
        }
        return p1 + "." + p2 + "." + p3 + ".0";
    }
    /**
     * 生成下一个IP
     */
    public static String generateNextIp(String currentIp) {
        // 1. 解析原始IP
        String[] ipSegments = currentIp.split("\\.");
        int seg1 = Integer.parseInt(ipSegments[0]);
        int seg2 = Integer.parseInt(ipSegments[1]);
        int seg3 = Integer.parseInt(ipSegments[2]);
        int seg4 = Integer.parseInt(ipSegments[3]);

        // 记录原始段1、段2（用于对比变化）
        int originalSeg1 = seg1;
        int originalSeg2 = seg2;

        // 2. 完整的IP自增进位逻辑（支持seg4→seg3→seg2→seg1全进位）
        seg4++;
        // 段4进位到段3
        if (seg4 > 255) {
            seg4 = 0;
            seg3++;
            // 段3进位到段2
            if (seg3 > 255) {
                seg3 = 0;
                seg2++;
                // 段2进位到段1（新增完整进位逻辑）
                if (seg2 > 255) {
                    seg2 = 0;
                    seg1++;
//                    // 可选：段1上限控制（IP合法范围0-255）
//                    if (seg1 > 255) {
//                        throw new IllegalArgumentException("IP已达到最大值：255.255.255.255");
//                    }
                }
            }
        }

        // 3. 检测段1/段2变化并打印
        boolean seg1Changed = seg1 != originalSeg1;
        boolean seg2Changed = seg2 != originalSeg2;
        if (seg1Changed || seg2Changed) {
            System.out.println("全局ip进程："+currentIp+"->"+String.format("%d.%d.%d.%d", seg1, seg2, seg3, seg4));
        }

        // 4. 返回新IP
        return String.format("%d.%d.%d.%d", seg1, seg2, seg3, seg4);
    }
    /**
     * 多线程安全的核心方法：判断currentIp是否超过endIp
     * @return true=超过（退出），false=相等/未到（继续），IP错误=true（兜底退出）
     */
    public static boolean isCurrentIpExceedEndIp(String currentIp, String endIp) {
        //获取当前线程独享的数组（无锁、无竞争）
        int[] ipParts = IP_PARTS_THREAD_LOCAL.get();

        //解析currentIp（线程独享数组，无覆盖）
        long currentIpNum;
        try {
            currentIpNum = parseIpToLong(currentIp, ipParts);
        } catch (IllegalArgumentException e) {
            return true; // IP格式错误，返回true退出
        }

        //解析endIp
        long endIpNum;
        try {
            endIpNum = parseIpToLong(endIp, ipParts);
        } catch (IllegalArgumentException e) {
            return true; // IP格式错误，返回true退出
        }

        //超过返回true
        return currentIpNum > endIpNum;
    }

    /**
     * 纯字符遍历解析IP为长整数（无拆串、无装箱、多线程安全）
     * @throws IllegalArgumentException IP格式错误时抛出
     */
    private static long parseIpToLong(String ip, int[] parts) {
        int len = ip.length();
        int partIndex = 0;
        int num = 0;

        for (int i = 0; i < len; i++) {
            char c = ip.charAt(i);
            if (c == '.') {
                // 校验分段数量和数值范围
                if (partIndex >= 3 || num < 0 || num > 255) {
                    throw new IllegalArgumentException("无效IP分段：" + ip);
                }
                parts[partIndex++] = num;
                num = 0;
            } else if (c >= '0' && c <= '9') {
                num = num * 10 + (c - '0');
                // 提前校验单段数值，避免溢出
                if (num > 255) {
                    throw new IllegalArgumentException("IP段数值超出范围：" + num);
                }
            } else {
                throw new IllegalArgumentException("IP包含非法字符：" + c);
            }
        }

        // 校验最后一段和总段数
        if (partIndex != 3 || num < 0 || num > 255) {
            throw new IllegalArgumentException("IP格式错误：" + ip);
        }
        parts[3] = num;

        // 纯位运算转长整数（效率最高，无乘法/除法）
        return ((long) parts[0] << 24)
                | ((long) parts[1] << 16)
                | ((long) parts[2] << 8)
                | parts[3];
    }

    public static List<IpSegment> generateIpSegments(int startNo) {
        List<IpSegment> list = new ArrayList<>();

        // 生成 5 个：startNo ~ startNo+4
        for (int i = 0; i < 5; i++) {
            int currentSeg = startNo + i;

            String startIp = currentSeg + ".0.0.0";
            String endIp = currentSeg + ".255.255.255";

            list.add(new IpSegment(String.valueOf(currentSeg), startIp, endIp));
        }

        return list;
    }

    /**
     * 根据 currentIp 生成第四位 0~255 的固定IP列表
     * currentIp 第四位必须是 0，如 192.168.1.0
     */
    public static List<String> generateFixedIpList(String currentIp) {
        if (currentIp == null || !currentIp.endsWith(".0")) {
            return Collections.emptyList();
        }

        // 截取掉最后一位 0，拿到前缀 如 3.2.1.
        String prefix = currentIp.substring(0, currentIp.lastIndexOf(".") + 1);
        List<String> ipList = new ArrayList<>(256);

        // 生成 0 ~ 255
        for (int i = 0; i < 256; i++) {
            ipList.add(prefix + i);
        }

        return ipList;
    }

    /**
     * 从IP中只取【第一段】作为段号，直接返回 String
     * 例如：10.0.1.10 → 返回 "10"
     */
    public static String getSegNoByIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            throw new IllegalArgumentException("IP 不能为空");
        }
        return ip.split("\\.")[0]; // 只取第一段，直接返回String
    }

    public static void main(String[] args) {
        List<IpSegment> list = generateIpSegments(1);
        System.out.println(list);
    }
}
