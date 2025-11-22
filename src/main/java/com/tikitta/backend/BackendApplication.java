package com.tikitta.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BackendApplication {
 // 실
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
        System.out.println("hello world");
	}

}
