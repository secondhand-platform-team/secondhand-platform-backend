package com.secondhand.coreservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "auth-service", url = "${auth-service.url:http://kong:8000}")
public interface UserServiceClient {

    @PutMapping("/api/users/{userId}/free-sell-use/decrease")
    void decrementFreeSellUse(@PathVariable String userId);

    @GetMapping("/api/users/{userId}/free-sell-use")
    int getFreeSellUsed(@PathVariable String userId);
}
