package com.ring.cloud.facade.entity.ip;

import com.ring.cloud.facade.common.TaskTypeEnum;
import com.ring.welkin.common.utils.Snowflake;
import lombok.Data;

//线程任务对象
@Data
public class IpTaskEntity {
    private Long taskId = Snowflake.longId();
    private IpSegment ipSegment;
    private int pageSize = 0;//记录数
    private Long startTime;//记录开始时间

    private String taskType = TaskTypeEnum.IP_DOMAIN_NORMAL.name();

    // 大IP分段专用字段
    private String handleIp;
    private Integer startPage;
    private Integer endPage;
    private String loc;

    // 原有构造不动
    public IpTaskEntity() {}
    public IpTaskEntity(IpSegment ipSegment) {
        this.ipSegment = ipSegment;
    }

    public IpTaskEntity(String handleIp, String taskType) {
        this.handleIp = handleIp;
        this.taskType = taskType;
    }

}
