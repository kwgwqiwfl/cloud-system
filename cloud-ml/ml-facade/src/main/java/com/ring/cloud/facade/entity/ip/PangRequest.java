package com.ring.cloud.facade.entity.ip;

import com.ring.cloud.facade.entity.proxy.ProxyIp;
import lombok.Data;

@Data
public class PangRequest {
    private String currentIp;
    private ProxyIp proxy;

    public PangRequest(String currentIp, ProxyIp proxy) {
        this.currentIp = currentIp;
        this.proxy = proxy;
    }
}
