package com.ring.cloud.facade.common;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum TaskTypeEnum {
    IP_SEG,//正常轮询模式
    IP_SINGLE,//小ip 单任务模式
    IP_DOMAIN_LARGE,//大IP分段模式
    DOMAIN,//域名模式
    KEYWORD,//域名模式
    DEFAULT;

}
