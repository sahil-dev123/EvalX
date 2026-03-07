package com.evalx;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableCaching
@SpringBootApplication
public class EvalXApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvalXApplication.class, args);
    }
}
