package com.ring.cloud.facade.entity;

import lombok.Data;

import java.util.List;

@Data
public class IpDomainRequest {
    private String startIp;
    private String endIp;
}
