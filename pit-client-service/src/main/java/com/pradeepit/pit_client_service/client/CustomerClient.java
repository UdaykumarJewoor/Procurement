package com.pradeepit.pit_client_service.client;

import com.pradeepit.pit_client_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pit-auth-service", configuration = FeignClientConfig.class)
public interface CustomerClient {
    @GetMapping("/auth/me/id")
    String getCustomerId();
    @GetMapping("/customers/read/customerId/{id}")
    Boolean isCustomerPresent(@PathVariable("id") String id);

}
