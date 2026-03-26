package com.ring.cloud.facade.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Half {
    private String fullScore;
    private String halfScore;
    private String halfFull;
}
