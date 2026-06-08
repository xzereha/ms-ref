package com.microservices.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "vault.enabled=false"
})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
