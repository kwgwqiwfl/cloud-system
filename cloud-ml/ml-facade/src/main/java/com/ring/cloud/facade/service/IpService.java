package com.ring.cloud.facade.service;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.core.frame.IpRouteInit;
import com.ring.cloud.core.pojo.IpRouteConfig;
import com.ring.cloud.core.service.IpDomainService;
import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.config.IpGlobalProgressManager;
import com.ring.cloud.facade.entity.ip.IpGlobalProgress;
import com.ring.cloud.facade.entity.ip.IpImport;
import com.ring.cloud.facade.entity.ip.IpSegment;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import com.ring.cloud.facade.execute.IpDomain.impl.IpPageCheckHelper;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class IpService extends SeaCommon {

    private static final Random RANDOM = new Random();

    @Autowired
    private IpGlobalProgressManager progressManager;
    @Autowired
    private IpPageCheckHelper ipPageCheckHelper;
    @Autowired
    private IpDomainService ipDomainService;

    //启动指定ip任务
    public void startSingleIpList(List<String> ipList) {
        if(CollectionUtils.isEmpty(ipList))
            return;
        for(String ip:ipList){
            String lockKey = ip.trim();
//            if(IpRouteInit.IP_LEVEL_MAP.containsKey(lockKey)){
//                //记录哪些成功，哪些是大ip未成功用来提示
//                continue;
//            }
            if (GlobalTaskManager.isSegmentRunning(lockKey)) {
                throw new IllegalArgumentException("IP【" + lockKey + "】正在运行，禁止重复启动");
            }
            if (!GlobalTaskManager.occupySegment(lockKey)) {
                throw new IllegalArgumentException("IP【" + lockKey + "】加锁失败");
            }
            log.info("启动单个ip简单任务->"+ ip);
            handlerExecutor.execHandler(factory, progressManager, new IpTaskEntity(lockKey, TaskTypeEnum.IP_DOMAIN_SMALL.name()));
        }
    }

    //自动计算一次根据开始段号，生成5段任务
    public void batchIp(Integer startNo) {
        List<IpSegment> ipSegments = IpUtil.generateIpSegments(startNo);
        for(IpSegment ipSegment:ipSegments) {
            startSegIp(ipSegment);
        }
    }
    //一次根据startIp list开启多个段任务
    public void startIpList(List<String> startIpList) {
        if(startIpList == null)
            return;
        for(String startIp:startIpList){
            startSegIp(new IpSegment(startIp));
        }
    }
    // 启动ip段线程任务逻辑 根据参数判断启动逻辑
    public void startSegIp(IpSegment ipSegment) {
        // 从起始IP获取段号 → 直接拿 String，绝对不转 int
        String startIp = ipSegment.getStartIp();
        String segNo = IpUtil.getSegNoByIp(startIp); // 👈 直接返回String，不动它

        // 直接设置到segment，全程String
        ipSegment.setSegmentNo(segNo);

        // 终止IP为空 → 自动生成后三位255
        if (ipSegment.getEndIp() == null || ipSegment.getEndIp().isEmpty()) {
            ipSegment.setEndIp(segNo + ".255.255.255");
        }

        if (GlobalTaskManager.isSegmentRunning(segNo)) {
            throw new IllegalArgumentException("启动失败，当前ip段任务正在运行，不可重复执行");
        }
        if (!GlobalTaskManager.occupySegment(segNo)) {
            throw new IllegalArgumentException("启动失败，当前ip段任务加锁失败");
        }
        log.info("启动->"+ JSON.toJSONString(ipSegment));
        handlerExecutor.execHandler(factory, progressManager, new IpTaskEntity(ipSegment));
    }

    //正在运行的ip段
    public Set<String> runningSeg() {
        Set<String> runningList = GlobalTaskManager.getRunningSegments();
        log.info("运行中IP段：" + runningList);
        return runningList;
    }
    // 导入临时表
    public Map<String, Object> patchInsertTmp(IpImport ipImport) {
        Map<String, Object> map = new HashMap<>();
        long start = System.currentTimeMillis();
        int totalRows = 0;

        try {
            String filePath = ipImport.getFilePath();
            String tableName = ipImport.getTableName();
            if (StringUtils.hasText(filePath)) {
                // 有 filePath：读取目录下所有文件，循环导入
                File dir = new File(filePath);
                if (!dir.isDirectory()) {
                    throw new RuntimeException("filePath 不是有效目录：" + filePath);
                }

                File[] files = dir.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
                if (files == null || files.length == 0) {
                    map.put("code", 500);
                    map.put("msg", "目录下无文件");
                    return map;
                }

                // 循环批量插入所有文件
                for (File file : files) {
                    String fileName = file.getAbsolutePath();
                    int rows = ipDomainService.patchInsert(tableName, fileName);
                    totalRows += rows;
                    log.info("文件导入完成：{}  行数：{}", fileName, rows);
                }

            } else {
                // 没有 filePath：保持原来逻辑（单个文件）
                String fileName = ipImport.getFileName();
                totalRows = ipDomainService.patchInsert(tableName, fileName);
            }
            // ======================================================

            map.put("code", 200);
            map.put("msg", "导入成功");
            map.put("importRows", totalRows);
            log.info("总导入行数：{}  总耗时：{}ms  表：{}", totalRows, (System.currentTimeMillis() - start), tableName);

        } catch (Exception e) {
            log.error("批量导入异常", e);
            map.put("code", 500);
            map.put("msg", "导入失败：" + e.getMessage());
        }
        return map;
    }

    // ====================== 核心配置 ======================
    private static final int DATA_PER_GRADE = 100000;       // 1档位 = 10万条
    private static final int PAGE_SIZE = 100;               // 每页条数（你自己的实际值）
    private static final int MAX_SEGMENTS = 20;             // 最大20线程
    private static final float SAFETY_EXPAND_RATE = 1.2f;   // 扩容20%，覆盖突增数据
    private static final int ABSOLUTE_MAX_PAGE = 50000;     // 安全封顶

    // ====================== 最终业务方法 ======================
    public void startSingleIpAutoSplit1(String targetIp) {
        String lockKey = targetIp.trim();

        // ====================== 1. 从初始化缓存获取档位 ======================
        Integer grade = IpRouteInit.IP_LEVEL_MAP.get(lockKey);

        // ====================== 2. 关键规则：查不到 / 0 → 直接返回 ======================
        if (grade == null || grade <= 0) {
            log.info("IP【{}】无档位或档位=0，判定为【非大IP】，不执行分段任务", lockKey);
            throw new IllegalArgumentException("IP【" + lockKey + "】非大IP，无需分段执行");
        }

        // ====================== 以下只有档位 ≥1 才会执行 ======================
        // 防重复启动
        if (GlobalTaskManager.isSegmentRunning(lockKey)) {
            throw new IllegalArgumentException("IP【" + lockKey + "】正在运行，禁止重复启动");
        }
        if (!GlobalTaskManager.occupySegment(lockKey)) {
            throw new IllegalArgumentException("IP【" + lockKey + "】加锁失败");
        }

        try {
            // 计算总页数
            long totalData = (long) grade * DATA_PER_GRADE;
            int estimateMaxPage = (int) ((totalData + PAGE_SIZE - 1) / PAGE_SIZE);

            // 安全扩容 + 封顶
            estimateMaxPage = Math.min((int) (estimateMaxPage * SAFETY_EXPAND_RATE), ABSOLUTE_MAX_PAGE);

            // 20段动态均分
            int pagesPerSegment = (estimateMaxPage + MAX_SEGMENTS - 1) / MAX_SEGMENTS;

            // 初始化进度
            progressManager.initTask(lockKey, MAX_SEGMENTS, estimateMaxPage);

            // 提交20个分段任务
            for (int i = 0; i < MAX_SEGMENTS; i++) {
                int start = i * pagesPerSegment + 1;
                int end = Math.min((i + 1) * pagesPerSegment, estimateMaxPage);

                if (start > estimateMaxPage) {
                    break;
                }

                IpTaskEntity task = new IpTaskEntity();
                task.setTaskType(TaskTypeEnum.IP_DOMAIN_LARGE.name());
                task.setHandleIp(lockKey);
                task.setStartPage(start);
                task.setEndPage(end);

                handlerExecutor.execHandler(factory, progressManager, task);
            }

            log.info("IP【{}】大IP任务启动完成，档位：{}，预估页数：{}，分20段执行", lockKey, grade, estimateMaxPage);

        } catch (Exception e) {
            GlobalTaskManager.releaseSegment(lockKey);
            log.error("IP【{}】大IP任务启动失败", lockKey, e);
            throw e;
        }
    }

    /**
     * 每段页数：600 页（你确定的最终值）
     */
    private static final int PER_SEGMENT = 600;
    /**
     * 对外：启动单个大IP，自动探测 + 自动分段并行
     */
    public void startSingleIpAutoSplit(String targetIp) {
        String lockKey = targetIp.trim();

        // 防重复启动（线程安全）
        if (GlobalTaskManager.isSegmentRunning(lockKey)) {
            throw new IllegalArgumentException("IP【" + lockKey + "】正在运行，禁止重复启动");
        }
        if (!GlobalTaskManager.occupySegment(lockKey)) {
            throw new IllegalArgumentException("IP【" + lockKey + "】加锁失败");
        }

        try {
            // 随机探测最大页
            Map<String, Object> detectResult = detectRandomMaxPage(lockKey);
            int estimateMaxPage = (int) detectResult.get("maxPage");
            String loc = (String) detectResult.get("loc");
            // 计算总段数
            int totalSegments = (estimateMaxPage + PER_SEGMENT - 1) / PER_SEGMENT;
            // 预判断队列容量
            if (!handlerExecutor.canAccept(totalSegments)) {
                throw new IllegalStateException(
                        "IP【" + lockKey + "】提交失败：队列剩余空间不足，需" + totalSegments + "个位置");
            }
            // 初始化全局进度
            progressManager.initTask(lockKey, totalSegments, estimateMaxPage);

            // 一次性提交所有分段
            for (int i = 0; i < totalSegments; i++) {
                int start = i * PER_SEGMENT + 1;
                int end = Math.min((i + 1) * PER_SEGMENT, estimateMaxPage);

                IpTaskEntity task = new IpTaskEntity();
                task.setTaskType(TaskTypeEnum.IP_DOMAIN_LARGE.name());
                task.setHandleIp(lockKey);
                task.setStartPage(start);
                task.setEndPage(end);
                task.setLoc(loc);

                handlerExecutor.execHandler(factory, progressManager, task);
            }

        } catch (Exception e) {
            // 异常释放锁
            GlobalTaskManager.releaseSegment(lockKey);
            throw e;
        }
    }

    /**
     * 随机探测逻辑：
     * 1. 从 400~420 随机一页开始
     * 2. 倍增后再随机偏移，不使用固定值
     */
    private Map<String, Object> detectRandomMaxPage(String ip) {
        Map<String, Object> result = new HashMap<>();
        int current = RANDOM.nextInt(21) + 400;
        int lastValid = current;
        String finalLoc = null;
        final int MAX_LIMIT = 200000;

        while (current < MAX_LIMIT) {
            // -------------- 核心：一次调用拿到 hasData + loc --------------
            Map<String, Object> check = ipPageCheckHelper.checkHasDataAndLoc(ip, current);
            boolean hasData = (boolean) check.get("hasData");
            String loc = (String) check.get("loc");

            if (hasData) {
                lastValid = current;
                if (finalLoc == null) {
                    finalLoc = loc; // 只存第一次拿到的loc
                }

                long nextVal = (long) current * 2;
                int offset = Math.max(1, (int) (nextVal * 0.05));
                current = (int) (nextVal - offset + RANDOM.nextInt(offset * 2 + 1));
            } else {
                break;
            }
        }

        int maxPage = Math.min(lastValid + PER_SEGMENT, MAX_LIMIT);
        result.put("maxPage", maxPage);
        result.put("loc", finalLoc);
        return result;
    }

    //ip进度查询
    public IpGlobalProgress getLargeIpProgress(String targetIp) {
        return progressManager.getProgress(targetIp);
    }

    // ip进度查询 带格式化进度（直接返回百分比，前端/日志都能用）
    public String getLargeIpProgressDesc(String targetIp) {
        IpGlobalProgress progress = progressManager.getProgress(targetIp);
        if (progress == null) {
            return "IP=" + targetIp + " | 任务未初始化";
        }

        int totalSeg = progress.getTotalSegments().get();
        int finishSeg = progress.getFinishedSegments().get();
        long totalPage = progress.getTotalPageEstimate().get();
        long finishPage = progress.getTotalPageFinished().get();

        double segRate = totalSeg > 0 ? finishSeg * 100.0 / totalSeg : 0;
        double pageRate = totalPage > 0 ? finishPage * 100.0 / totalPage : 0;

        return String.format("IP=%s | 分段进度=%d/%d(%.1f%%) | 页数进度=%d/%d(%.1f%%)",
                targetIp,
                finishSeg, totalSeg, segRate,
                finishPage, totalPage, pageRate);
    }
    //同步ip路由
    public void syncIpRoute(IpRouteConfig ipRouteConfig) {
        IpRouteInit.syncIpRoute(ipRouteConfig);
    }
    /**
     * 统一停止：支持传入【段号】或【IP】，自动识别并停止
     */
    public String stopTask(String key) {
        boolean hasTask = false;

        // 先尝试停止【普通段任务】
        String normalKey = TaskTypeEnum.IP_DOMAIN_NORMAL.name() + ":" + key;
        AtomicBoolean normalFlag = GlobalTaskManager.TASK_STOP_MAP.get(normalKey);
        if (normalFlag != null) {
            normalFlag.set(true);
            log.info("已停止普通IP段任务：{}", key);
            hasTask = true;
        }

        // 再尝试停止【大IP自动分段任务】
        String largeKey = TaskTypeEnum.IP_DOMAIN_LARGE.name() + ":" + key;
        AtomicBoolean largeFlag = GlobalTaskManager.TASK_STOP_MAP.get(largeKey);
        if (largeFlag != null) {
            largeFlag.set(true);
            progressManager.stopTask(key);
            log.info("已停止大IP自动任务：{}", key);
            hasTask = true;
        }

        // 最后统一释放全局锁
        GlobalTaskManager.releaseSegment(key);

        // ========== 新增判断逻辑 ==========
        if (hasTask) {
            return " 任务停止成功";
        } else {
            return " 任务不存在";
        }
    }

    public Map<String, Object> getIpProgress(String targetIp) {
        Map<String, Object> result = new HashMap<>();

        // 从你真实的进度管理器获取
        IpGlobalProgress progress = progressManager.getProgress(targetIp);

        if (progress == null) {
            result.put("ip", targetIp);
            result.put("msg", "暂无进度");
            result.put("status", "NOT_EXIST");
            return result;
        }

        // 真实字段，完全匹配你的类
        result.put("ip", targetIp);
        result.put("totalSegments", progress.getTotalSegments().get());
        result.put("finishedSegments", progress.getFinishedSegments().get());
        result.put("currentRunning", progress.getCurrentRunning().get());
        result.put("totalPageEstimate", progress.getTotalPageEstimate().get());
        result.put("totalPageFinished", progress.getTotalPageFinished().get());
        result.put("isStopped", progress.isStopped());

        // 计算百分比（避免除0）
        int totalSeg = progress.getTotalSegments().get();
        int finishedSeg = progress.getFinishedSegments().get();
        if (totalSeg > 0) {
            int percent = (finishedSeg * 100) / totalSeg;
            result.put("progressPercent", percent+"%");
        } else {
            result.put("progressPercent", 0);
        }

        return result;
    }
}
