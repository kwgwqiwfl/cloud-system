package com.ring.cloud.facade.entity.ip;

import lombok.Data;

import java.util.List;

/**"code": 200,
        "msg": "成功",
        "data": {
                 "ip": 3419416727,
                 "addtime": "20230501",
                 "uptime": "20260315"
             }
 */
//分页返回基础信息
@Data
public class DomainPageResponse {
    private int code;//状态码
    private String msg;//成功失败信息
    private List<DomainPageData> data;//详细信息
}
