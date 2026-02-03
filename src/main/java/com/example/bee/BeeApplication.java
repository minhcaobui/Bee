package com.example.bee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeeApplication.class, args);
	}

}
