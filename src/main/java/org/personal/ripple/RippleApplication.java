package org.personal.ripple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RippleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RippleApplication.class, args);
    }

}
