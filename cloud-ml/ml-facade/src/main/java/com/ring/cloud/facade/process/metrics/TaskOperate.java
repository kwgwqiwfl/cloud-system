package com.ring.cloud.facade.process.metrics;

import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.config.PangIpServcie;
import com.ring.cloud.facade.entity.proxy.ProxyIp;
import com.ring.cloud.facade.frame.OkProxyBase;
import com.ring.cloud.facade.proxy.GlobalProxyHelper;
import com.ring.cloud.facade.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class TaskOperate<T> implements ITaskManage<T> {
    @Autowired
    protected OkProxyBase okProxyBase;
    @Autowired
    protected PangIpServcie pangIpServcie;
    @Autowired
    protected GlobalProxyHelper globalProxyHelper;

    @Value("${ml.client.ip.file.path:/}")
    protected String ipFilePath;//文件路径
    @Value("${ml.client.ip.file.name.prefix:test}")
    protected String ipFileNamePrefix;//文件名前缀

    protected ProxyIp getAvailableProxy() {
        return globalProxyHelper.getAvailableProxy();
    }

    protected boolean needSwitchProxy(Throwable e) {
        return globalProxyHelper.needSwitchProxy(e);
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
     * 关闭流并重命名文件
     */
    protected void closeCsv(BufferedWriter bw, String tmpPath, String csvPath, boolean isBatchComplete) throws IOException {
        try {
            if (bw != null) {
                bw.flush();
                bw.close();
            }
        } catch (Exception ex) {
            log.error("关闭CSV文件流失败", ex);
        }

        if (isBatchComplete) {
            FileUtil.renameTmpToCsv(tmpPath, csvPath);
        }
    }


}
