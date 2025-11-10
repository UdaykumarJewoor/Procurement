package com.pradeepit.pit_client_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PitClientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PitClientServiceApplication.class, args);
	}

}
