package com.ring.cloud.facade.entity.ip;

import com.ring.cloud.core.pojo.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MixIpInfo {

    private List<MlIp> ipList;

    private List<MlDomain> domainList;

    private List<MlIcp> icpList;

    private List<MlSubdomain> subdomainList;

    private List<MixIpDomain> mixIpDomainList = new ArrayList<>();

    private List<MixDomainIp> mixDomainIpList = new ArrayList<>();

    private boolean success = false;//成功失败

}