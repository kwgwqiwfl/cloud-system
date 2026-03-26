package com.ring.cloud.core.entity.ip;

import lombok.Data;

@Data
public class IpTaskInfo implements Cloneable{
    String startIp;
    String endIp;

    @Override
    public IpTaskInfo clone() throws CloneNotSupportedException {
        return (IpTaskInfo) super.clone();
    }
}
