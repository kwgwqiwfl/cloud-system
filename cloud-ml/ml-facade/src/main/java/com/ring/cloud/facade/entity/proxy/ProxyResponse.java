package com.ring.cloud.facade.entity.proxy;

import lombok.Data;
/**"code": 200,
        "msg": "成功",
        "data": {
            "count": 10,
            "filter_count": 0,
            "surplus_quantity": 0,
            "proxy_list":[]
        }
 */
//代理返回信息
@Data
public class ProxyResponse {
    private int code;//状态码
    private String msg;//成功失败信息
    private ProxyData data;//详细信息
}
