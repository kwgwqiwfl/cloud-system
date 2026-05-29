package com.ring.cloud.auth.feign;

import com.ring.cloud.auth.feign.dto.UserCheckRequestDTO;
import com.ring.welkin.common.core.ml.MResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("user-service")
public interface UserFeignClient {

    @PostMapping("/sys/user/inner/check")
    MResponse<?> checkUser(@RequestBody UserCheckRequestDTO dto);
}