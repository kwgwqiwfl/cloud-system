package com.ring.cloud.facade.process.metrics;

import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.IpBreakpoint;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkProxyPang;
import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import com.ring.cloud.facade.support.PangIpSupport;
import com.ring.cloud.facade.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public abstract class AbstractTask<T> implements ITask<T> {
    @Autowired
    protected OkProxyPang okProxyBase;
    @Autowired
    protected PangIpSupport pangIpServcie;
    @Autowired
    protected GlobalProxyHelper globalProxyHelper;

    @Value("${ml.client.ip.file.path:/}")
    protected String ipFilePath;//文件路径
    @Value("${ml.client.ip.file.name.prefix:test}")
    protected String ipFileNamePrefix;//文件名前缀

    /**
     * 通用IP 域名等采集重试 + 代理切换 + 任务停止
     */
    protected void retryExecute(
            String uniqueKey,
            ProxyIp currentProxy,
            IpBreakpoint breakpoint,
            String key,
            BufferedWriter bw,
            int maxRetry
    ) {
        int retryCount = 0;

        while (true) {
            if (isTaskStopped(uniqueKey)) {
                log.info("任务[" + uniqueKey + "]已终止，最后处理：" + key);
                return;
            }
            if (currentProxy == null) {
                currentProxy = getAvailableProxy();
            }

            try {
                // 执行业务（子类实现）
                if (doExecute(key, bw, currentProxy, breakpoint)) {
                    return;
                }
                // 失败计数 + 切换代理
                retryCount++;
                currentProxy = null;
                log.debug("【】从断点 {} 继续，失败次数：{}", breakpoint.getCurrentPage(), retryCount);

                if (retryCount >= maxRetry) {
                    log.warn("【】查询失败，忽略重试：{} -- {}", key, maxRetry);
                    return;
                }
            } catch (Throwable e) {
                retryCount++;  //异常计数
                boolean needSwitch = globalProxyHelper.needSwitchProxy(e);
                if (needSwitch) {
                    currentProxy = null;
                    log.debug("【代理异常】切换代理，失败次数：{}", retryCount);
                } else {
                    log.debug("【目标波动】不切换代理，失败次数：{}", retryCount);
                }

                //异常也判断最大次数
                if (retryCount >= maxRetry) {
                    log.warn("【】异常超限，退出重试：{} -- {}", key, maxRetry);
                    return;
                }
            }
        }
    }
    /**
     * 通用重试查询方法
     */
    protected final <R> R queryWithRetry(String key, ProxyIp proxy, int maxRetry, BiFunction<String, ProxyIp, R> queryFunc, Function<String, R> defaultFunc) {
        int retryCount = 0;
        R result = null;

        while (retryCount < maxRetry) {
            try {
                result = queryFunc.apply(key, proxy);
                if (result != null) break;
            } catch (Throwable e) {
                log.debug("查询异常，重试：{}", retryCount);
            }

            retryCount++;
            if (retryCount < maxRetry) {
                proxy = getAvailableProxy();
            }
        }

        if (result == null) {
            result = defaultFunc.apply(key);
        }
        return result;
    }
    /**
     * 子类重写：执行自己的 execute 业务
     */
    protected boolean doExecute(String key, BufferedWriter bw, ProxyIp currentProxy, IpBreakpoint breakpoint) throws IOException{
        return false;
    }

    protected ProxyIp getAvailableProxy() {
        return globalProxyHelper.getAvailableProxy();
    }

    /**
     * 初始化CSV写入流
     */
    protected BufferedWriter initCsvWriter(String tmpCsvPath) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(tmpCsvPath),
                        StandardCharsets.UTF_8
                )
        );
    }

    protected boolean isTaskStopped(String uniqueKey) {
        if (uniqueKey == null) return false;
        AtomicBoolean flag = GlobalTaskManager.TASK_STOP_MAP.get(uniqueKey);
        return flag != null && flag.get();
    }

    protected void markTaskStop(String uniqueKey) {
        if (uniqueKey != null) {
            GlobalTaskManager.TASK_STOP_MAP.put(uniqueKey, new AtomicBoolean(true));
        }
    }

    protected void clearTaskStopFlag(String uniqueKey) {
        if (uniqueKey != null) {
            GlobalTaskManager.TASK_STOP_MAP.remove(uniqueKey);
        }
    }

    /**
     * 关闭流并重命名文件 文件路径
     */
    protected void closeFileAndRenameByPath(BufferedWriter bw, String tmpPath, String csvPath, boolean isBatchComplete) throws IOException {
        try {
            if (bw != null) {
                bw.flush();
                bw.close();
            }
        } catch (Exception ex) {
            log.error("关闭文件流失败", ex);
        }

        if (isBatchComplete) {
            FileUtil.renameTmpToFile(tmpPath, csvPath);
        }
    }
    /**
     * 关闭流并重命名文件 文件
     */
    protected void closeFileAndRenameByFile(BufferedWriter bw, File tmpFile, File finalFile) {
        try {
            if (bw != null) {
                bw.flush();
                bw.close();
            }
        } catch (Exception e) {
            log.error("关闭文件失败", e);
        }
        if (tmpFile.exists()) {
            tmpFile.renameTo(finalFile);
        }
    }


}
