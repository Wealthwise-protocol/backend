package com.wealthwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WealthwiseBackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(WealthwiseBackendApplication.class);
		app.run(args);
	}

}
