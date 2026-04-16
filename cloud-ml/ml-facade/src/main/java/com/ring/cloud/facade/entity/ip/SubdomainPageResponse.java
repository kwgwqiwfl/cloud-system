package com.ring.cloud.facade.entity.ip;

import lombok.Data;

import java.util.List;

/**""status": true,
     "code": 0,
     "msg": "",
     "data": {
         "domain": "amazon.com",
         "page": 4,
         "pageSize": 50,
         "result": [
             "sellcentral.amazon.com",
             "smtp-border-fw-out-33001.sea14.amazon.com",
             "sellingpartnerapi-fe.amazon.com",
 */
//分页返回基础信息
@Data
public class SubdomainPageResponse {
    private boolean status = false;
    private int code;//状态码
    private String msg;//信息
    private SubdomainPageData data;//详细信息
}
