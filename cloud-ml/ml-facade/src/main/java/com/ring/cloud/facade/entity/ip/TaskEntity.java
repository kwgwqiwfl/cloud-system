package com.ring.cloud.facade.entity.ip;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.welkin.common.utils.Snowflake;
import lombok.Data;

import java.util.List;

//线程任务对象
@Data
public class TaskEntity {
    private Long taskId = Snowflake.longId();
    private IpSegment ipSegment;
    private int pageSize = 0;//记录数
    private Long startTime;//记录开始时间

    private String taskType = TaskTypeEnum.IP_SEG.name();

    // 特殊任务专用字段
    private String handleKey;
    private Integer startPage;
    private Integer endPage;
    private String loc;

    // domain任务专用字段
    private List<String> handleKeyList;

    // 关键词任务专用字段
    private String site;

    // 原有构造不动
    public TaskEntity() {}
    public TaskEntity(IpSegment ipSegment) {
        this.ipSegment = ipSegment;
    }

    public TaskEntity(String handleIp, String taskType) {
        this.handleKey = handleIp;
        this.taskType = taskType;
    }

}
