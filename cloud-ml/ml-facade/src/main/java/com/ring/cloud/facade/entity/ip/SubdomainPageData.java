package com.ring.cloud.facade.entity.ip;

import lombok.Data;

import java.util.List;

//"domain": "amazon.com",
//         "page": 4,
//         "pageSize": 50,
//         "result": [
//             "sellcentral.amazon.com",
//             "smtp-border-fw-out-33001.sea14.amazon.com",
//             "sellingpartnerapi-fe.amazon.com",
@Data
public class SubdomainPageData {
    int pageSize;
    List<String> result;
}
