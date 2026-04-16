package com.ring.cloud.facade.entity.ip;

import lombok.Data;

//分页返回 内部data对象
@Data
public class DomainPageData {
    private long ip;
    private String addtime;
    private String uptime;
}
