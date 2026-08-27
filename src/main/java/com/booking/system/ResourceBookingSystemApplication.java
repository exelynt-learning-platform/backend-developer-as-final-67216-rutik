package com.booking.system;

import com.booking.system.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ResourceBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceBookingSystemApplication.class, args);
    }
}
