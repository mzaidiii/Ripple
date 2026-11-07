package org.personal.ripple;

import org.personal.ripple.llm.EmbeddingClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@EnableScheduling
public class RippleApplication {

    public static void main(String[] args) {SpringApplication.run(RippleApplication.class, args);}
}
