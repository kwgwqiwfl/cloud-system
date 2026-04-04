package com.ring.cloud.facade.common;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum TaskTypeEnum {
    IP_DOMAIN_NORMAL,//正常轮询模式
    IP_DOMAIN_SMALL,//小ip 单任务模式
    IP_DOMAIN_LARGE;//大IP分段模式

}
