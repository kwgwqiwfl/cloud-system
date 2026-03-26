package com.ring.cloud.facade.entity.ip;

import lombok.Data;

//分页返回 内部data对象
@Data
public class IpPageData {
    private String domain;
    private String addtime;
    private String uptime;
}
