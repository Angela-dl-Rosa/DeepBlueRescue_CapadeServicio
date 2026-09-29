package com.deepblue.rescue;

import org.springframework.boot.SpringApplication;

public class TestRescueApplication {

	public static void main(String[] args) {
		SpringApplication.from(RescueApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
