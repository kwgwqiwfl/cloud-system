package com.ring.cloud.facade.entity.proxy;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

//代理返回信息 内部data对象
@Data
public class ProxyData {
    private int count;
    private int filter_count;
    private int surplus_quantity;
    private List<String> proxy_list = new ArrayList<>();//地址信息
}
