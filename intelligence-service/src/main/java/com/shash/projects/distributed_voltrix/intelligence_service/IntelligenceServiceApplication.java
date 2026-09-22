package com.shash.projects.distributed_voltrix.intelligence_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class IntelligenceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntelligenceServiceApplication.class, args);
	}

}
