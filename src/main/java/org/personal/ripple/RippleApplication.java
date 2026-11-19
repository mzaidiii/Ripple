package org.personal.ripple;

import org.personal.ripple.llm.LlmClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class RippleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RippleApplication.class, args);
    }
    @Bean
    CommandLineRunner llmTest(LlmClient llm) {
        return args -> {
            for (int i = 0; i < 2; i++) {
                long t0 = System.currentTimeMillis();
                String answer = llm.complete("Reply with exactly five words.");
                System.out.println(answer + "  (" + (System.currentTimeMillis() - t0) + " ms)");
            }
        };
    }
    }
