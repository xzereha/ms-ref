package com.microservices.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.gateway.enabled=false",
    "vault.enabled=false"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
