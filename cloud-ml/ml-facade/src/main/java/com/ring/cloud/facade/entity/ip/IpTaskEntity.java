package com.ring.cloud.facade.entity.ip;

import com.ring.welkin.common.utils.Snowflake;
import lombok.Data;

//线程任务对象
@Data
public class IpTaskEntity {
    private Long taskId = Snowflake.longId();
    private IpSegment ipSegment;
    private int pageSize = 0;//记录数
    private Long startTime;//记录开始时间

    public IpTaskEntity() {
    }

    public IpTaskEntity(IpSegment ipSegment) {
        this.ipSegment = ipSegment;
    }
}
