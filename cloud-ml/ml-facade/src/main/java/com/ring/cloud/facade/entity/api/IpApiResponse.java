package com.ring.cloud.facade.entity.api;

import lombok.Data;

@Data
public class IpApiResponse {
    private Boolean status;
    private Integer code;
    private String msg;
    private String id;
    private DomainData data;
}
