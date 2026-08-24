package com.getvaas.distribution.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
public class DistributionEngineApplication {

	private final Environment environment;

	public DistributionEngineApplication(Environment environment) {
		this.environment = environment;
	}

	public static void main(String[] args) {
		SpringApplication.run(DistributionEngineApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		String port = environment.getProperty("server.port", "8080");
		String profiles = String.join(", ", environment.getActiveProfiles());
		if (profiles.isBlank()) profiles = "default";
		log.info("Server started on port {} — profile: {}", port, profiles);
	}

}
