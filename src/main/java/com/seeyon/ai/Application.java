package com.seeyon.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import com.seeyon.boot.annotation.BootApp;

@SpringBootApplication
@EnableCaching
@BootApp(name = "ai-low-code-assistant", caption = "AI低代码助手")
public class Application {
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "local");
        SpringApplication.run(Application.class, args);
    }
} 