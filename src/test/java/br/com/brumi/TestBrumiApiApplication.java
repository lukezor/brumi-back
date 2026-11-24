package br.com.brumi;

import org.springframework.boot.SpringApplication;

public class TestBrumiApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(BrumiApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
