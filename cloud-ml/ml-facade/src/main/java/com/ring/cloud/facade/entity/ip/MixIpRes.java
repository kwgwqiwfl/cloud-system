package com.ring.cloud.facade.entity.ip;

import com.ring.cloud.core.pojo.MixDomainIp;
import com.ring.cloud.core.pojo.MixIpDomain;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MixIpRes {
    private String currentIp;//当前ip
    private String loc;//归属
    private String token = "";
    private boolean success = false;//成功失败
    private List<MixIpDomain> mixIpDomainList = new ArrayList<>();

    private List<MixDomainIp> mixDomainIpList = new ArrayList<>();
}
