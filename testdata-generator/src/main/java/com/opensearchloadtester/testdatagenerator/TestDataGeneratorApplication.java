package com.opensearchloadtester.testdatagenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TestDataGeneratorApplication {

	public static void main(String[] args) {

		ConfigurableApplicationContext ctx = SpringApplication.run(TestDataGeneratorApplication.class, args);


		try {
			Thread.sleep(10000); // 10 seconds
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// exit gracefully with code 0 to indicate success (so Compose sees service_completed_successfully)
		int exitCode = SpringApplication.exit(ctx, () -> 0);
		System.exit(exitCode);
	}

}
