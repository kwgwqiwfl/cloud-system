package com.ring.cloud.facade.execute;

import cn.hutool.extra.spring.SpringUtil;
import com.ring.cloud.facade.common.TaskFactory;
import com.ring.cloud.facade.config.GlobalTaskManager;
import com.ring.cloud.facade.entity.ip.IpTaskEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TaskHandler implements IHandler {
    private TaskFactory factory = SpringUtil.getBean(TaskFactory.class);

    private String type = "FULL";
    private IpTaskEntity ipTaskEntity;

    public TaskHandler(IpTaskEntity ipTaskEntity) {
        ipTaskEntity.setStartTime(System.currentTimeMillis());
        this.ipTaskEntity = ipTaskEntity;
    }

    /**
     * @return 是否执行结束循环; true-执行结束；false-未结束
     */
    @Override
    public boolean handle() {
        int segNo = ipTaskEntity.getIpSegment().getSegmentNo();
        System.out.println("任务开始，ip段="+segNo);
        try {
//            System.out.println("handle 开始");
            GlobalTaskManager.TASK_STOP_MAP.put(segNo, new AtomicBoolean(false));
            boolean status = factory.taskManage(type).runTask(ipTaskEntity);
            System.out.println("任务结束 id="+ipTaskEntity.getTaskId()+"  状态："+status +" 耗时："+(System.currentTimeMillis()-ipTaskEntity.getStartTime()+" ms"));
//            if(status)//监控任务结束
//                log.debug("handleJob success end. jobId="+taskExecution.getJobId()+". name="+taskExecution.getName());
            return status;
        } catch (Throwable e) {
//            System.out.println("handle 执行失败："+e.getMessage());
//            log.error(String.format("jobName:%s get status error %s", taskExecution.getName(), e.getMessage()));
            return false;
        }finally {
            GlobalTaskManager.TASK_STOP_MAP.remove(segNo);
            GlobalTaskManager.releaseSegment(segNo);
        }
    }
}
