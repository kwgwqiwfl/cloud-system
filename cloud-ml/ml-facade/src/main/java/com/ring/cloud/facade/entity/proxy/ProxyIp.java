package com.ring.cloud.facade.entity.proxy;

import lombok.Data;

//代理解析ip信息
@Data
public class ProxyIp {
    private String ip;
    private int port;
}
