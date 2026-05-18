package com.ring.cloud.facade.entity.ip;

import lombok.Data;

@Data
public class PangIpData {
    private String ip;
    private String count;

    public PangIpData(String ip, String count) {
        this.ip = ip;
        this.count = count;
    }
}
