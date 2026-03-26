package com.ring.cloud.facade.service;

import com.alibaba.fastjson.JSON;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.IpDomainRequest;
import com.ring.cloud.facade.entity.ip.IpSegment;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import com.ring.cloud.facade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
//@Transactional
public class IpService extends SeaCommon {

    //启动ip线程任务逻辑 根据参数判断启动逻辑
    public void startIpTask(IpDomainRequest ipDomainRequest) {
        ipDomainRequest = new IpDomainRequest();
//        ipDomainRequest.setStartIp("0.0.0.0");
//        ipDomainRequest.setEndIp("255.255.255.255");
        //固定规则拆分ip 10段 每段对应一张表
        List<IpSegment> ipSegments = ipSegmentSplitConfig.splitIpSegments();
        log.info("==>"+JSON.toJSONString(ipSegments));
        for(IpSegment ipSegment:ipSegments) {
            handlerExecutor.execHandler(new IpTaskEntity(ipSegment));
        }
    }

    public void batchIp(Integer startNo) {
        List<IpSegment> ipSegments = IpUtil.generateIpSegments(startNo);
        for(IpSegment ipSegment:ipSegments) {
            startSingleIp(ipSegment);
        }
    }

    //启动ip段线程任务逻辑 根据参数判断启动逻辑
    public void startSingleIp(IpSegment ipSegment) {
        int segmentNo = ipSegment.getSegmentNo();
        if (GlobalTaskManager.isSegmentRunning(segmentNo)) {
            throw new IllegalArgumentException("启动失败，当前ip段任务正在运行，不可重复执行");
        }
        // 尝试加锁（这里再次判断，原子安全）
        if (!GlobalTaskManager.occupySegment(segmentNo)) {
            throw new IllegalArgumentException("启动失败，当前ip段任务加锁失败");
        }
        handlerExecutor.execHandler(new IpTaskEntity(ipSegment));
    }
    //停止ip段
    public void stopSingleIp(Integer segNo) {
        AtomicBoolean flag = GlobalTaskManager.TASK_STOP_MAP.get(segNo);
        if (flag != null) {
            flag.set(true);
        }
        // 解锁ip段
        GlobalTaskManager.releaseSegment(segNo);
    }
    //正在运行的段
    public Set<Integer> runningSeg() {
        Set<Integer> runningList = GlobalTaskManager.getRunningSegments();
        System.out.println("运行中IP段：" + runningList);
        return runningList;
    }


}
