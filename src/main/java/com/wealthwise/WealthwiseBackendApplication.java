package com.wealthwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WealthwiseBackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(WealthwiseBackendApplication.class);
		app.run(args);
	}

}
