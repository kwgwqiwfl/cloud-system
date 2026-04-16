package com.ring.cloud.core.entity.ip;

import lombok.Data;

@Data
public class DomainCount implements Cloneable{
    String inputDomain;
    String outputDomain;
    int count;

    @Override
    public DomainCount clone() throws CloneNotSupportedException {
        return (DomainCount) super.clone();
    }
}
