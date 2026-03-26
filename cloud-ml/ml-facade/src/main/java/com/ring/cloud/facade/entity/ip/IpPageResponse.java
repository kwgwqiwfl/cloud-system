package com.ring.cloud.facade.entity.ip;

import lombok.Data;

import java.util.List;

/**"code": 200,
        "msg": "成功",
        "data": {
                 "domain": "c2c.wechat.com",
                 "addtime": "20230501",
                 "uptime": "20260315"
             }
 */
//分页返回基础信息
@Data
public class IpPageResponse {
    private int code;//状态码
    private String msg;//成功失败信息
    private List<IpPageData> data;//详细信息
}
