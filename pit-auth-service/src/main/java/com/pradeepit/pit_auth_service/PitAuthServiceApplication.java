package com.pradeepit.pit_auth_service;

import com.pradeepit.pit_auth_service.component.RoleSeeder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PitAuthServiceApplication implements CommandLineRunner {

	@Autowired
	private RoleSeeder roleSeeder;

	public static void main(String[] args) {
		SpringApplication.run(PitAuthServiceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		roleSeeder.seedSuperAdminRole();
	}
}
