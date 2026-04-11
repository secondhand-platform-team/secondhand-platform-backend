package com.secondhand.coreservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Extract JWT token from SecurityContext
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            String token = authentication.getCredentials().toString();
            String authHeader = "Bearer " + token;
            template.header("Authorization", authHeader);
            log.debug("Added Authorization header to Feign request");
        } else {
            log.warn("No JWT token found in SecurityContext for Feign request");
        }
    }
}
