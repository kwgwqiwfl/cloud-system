package com.ring.cloud.facade.entity.ip;

import com.ring.cloud.core.pojo.MlDomain;
import com.ring.cloud.core.pojo.MlIcp;
import com.ring.cloud.core.pojo.MlIp;
import com.ring.cloud.core.pojo.MlSubdomain;
import lombok.Data;
import java.util.List;

@Data
public class MixIpInfo {

    private List<MlIp> ipList;

    private List<MlDomain> domainList;

    private List<MlIcp> icpList;

    private List<MlSubdomain> subdomainList;

}