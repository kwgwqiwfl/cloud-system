package com.ring.cloud.facade.entity.ip;

import lombok.Data;

//首次查询获取到的全局信息
@Data
public class IpReadInfo {
    private String currentIp;//当前ip
    private String loc;//归属
    private String token = "";
    private int pageSize = 0;//记录数
    private boolean success = false;//成功失败
}
