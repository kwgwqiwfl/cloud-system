package com.ring.cloud.facade.frame;

import com.ring.cloud.facade.entity.ip.PangRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
public class RetryTemplate {
    // ======================= 无参方法=======================
    public <T> T execute(int maxRetry, int intervalSec, Supplier<T> supplier, String bizName) {
        int retryCount = 0;
        while (retryCount < maxRetry) {
            try {
                return supplier.get();
            } catch (Throwable e) {
                log.debug("【{}】执行异常，次数：{}，原因：{}", bizName, retryCount, e.getMessage());
            }

            retryCount++;
            if (retryCount >= maxRetry) {
                log.debug("【{}】达到最大重试次数：{}", bizName, maxRetry);
                return null;
            }

            try {
                TimeUnit.SECONDS.sleep(intervalSec);
                log.debug("【{}】{}秒后重试，第{}次", bizName, intervalSec, retryCount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("【{}】重试休眠被中断", bizName);
                return null;
            }
        }
        return null;
    }

    public <T, R> T execute(int maxRetry, int intervalSec, Function<R, T> function, R param, String bizName) {
        int retryCount = 0;
        while (retryCount < maxRetry) {
            try {
                return function.apply(param);
            } catch (Throwable e) {
                log.debug("【{}】执行异常，次数：{}，原因：{}", bizName, retryCount, e.getMessage());
            }

            retryCount++;
            if (retryCount >= maxRetry) {
                log.debug("【{}】达到最大重试次数：{}", bizName, maxRetry);
                return null;
            }

            try {
                TimeUnit.SECONDS.sleep(intervalSec);
                log.debug("【{}】{}秒后重试，第{}次", bizName, intervalSec, retryCount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("【{}】重试休眠被中断", bizName);
                return null;
            }
        }
        return null;
    }
}
