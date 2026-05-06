package com.ring.cloud.facade.support;

import com.ring.cloud.facade.entity.api.DomainResult;
import com.ring.cloud.facade.entity.api.IpApiResponse;
import com.ring.cloud.facade.entity.api.IpLocationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IpApiClient {
    @Autowired
    protected RestTemplate restTemplate;

    @Value("${ml.client.ip.api.ipdomain:null}")
    private String ipDoaminUrl;
    @Value("${ml.client.ip.api.ipdata:null}")
    private String ipDataUrl;

    public List<DomainResult> queryDomainByIp(String ip, int page) {
        Map<String, Object> params = new HashMap<>();
        params.put("ip", ip);
        params.put("page", page);
        IpApiResponse apiResponse = get(ipDoaminUrl, "fed28348588dcbf730eb26a03bfc71ae", params, IpApiResponse.class);
        if (apiResponse != null && Boolean.TRUE.equals(apiResponse.getStatus())) {
            return apiResponse.getData().getResults();
        } else {
            throw new IllegalArgumentException(apiResponse.getMsg());
        }
    }

    public List<String> ipdata(String ip) {
        Map<String, Object> params = new HashMap<>();
        params.put("ip", ip);
        params.put("datatype", "json");
//        params.put("callback", "callback");
        IpLocationResponse locationResponse = get(ipDataUrl, "da8b5cc35034c4f369e7c091da88ca91", params, IpLocationResponse.class);
        if (locationResponse != null && "ok".equals(locationResponse.getRet())) {
            return locationResponse.getData();
        } else {
            String errMsg = locationResponse == null ? "请求失败" : locationResponse.getMsg();
            throw new IllegalArgumentException(errMsg);
        }
    }

    /**
     * 通用GET请求
     * @param url 请求地址
     * @param token 认证token
     * @param params 请求参数（可以传null）
     * @param clazz 返回类型
     * @return 解析后的对象
     */
    public <T> T get(String url, String token, Map<String, Object> params, Class<T> clazz) {
        // 拼接参数
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        String finalUrl = builder.toUriString();

        // 设置请求头 token
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        // 发送请求
        ResponseEntity<T> response = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                entity,
                clazz
        );

        return response.getBody();
    }
}
