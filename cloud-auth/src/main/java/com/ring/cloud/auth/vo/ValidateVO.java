package com.ring.cloud.auth.vo;

import lombok.Data;

@Data
public class ValidateVO {

    private boolean valid;

    private Long userId;
}