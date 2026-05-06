package com.ring.cloud.facade.entity.api;

import lombok.Data;

import java.util.List;

@Data
public class IpLocationResponse {

    // 状态：ok=成功 / err=失败
    private String ret;

    // 成功时返回
    private String ip;
    private List<String> data;

    // 失败时返回
    private String msg;
}