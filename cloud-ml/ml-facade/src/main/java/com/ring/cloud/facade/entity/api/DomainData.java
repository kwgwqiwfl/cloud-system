package com.ring.cloud.facade.entity.api;

import lombok.Data;

import java.util.List;

@Data
public class DomainData {
    private String ip;
    private Integer page;
    private Integer pageSize;
    private List<DomainResult> results;
}
