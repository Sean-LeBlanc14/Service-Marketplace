package com.ServiceMarketplace.service_marketplace;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class ServiceMarketplaceApplicationTests {

	private static final String JWT_TEST_KEY = Base64.getEncoder()
		.encodeToString(new byte[32]);

	@DynamicPropertySource
	static void registerTestProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.mongodb.uri", () -> "mongodb://localhost:27017/service_marketplace_test");
		registry.add("security.jwt.secret-key", () -> JWT_TEST_KEY);
		registry.add("security.jwt.expiration-time", () -> "3600000");
		registry.add("spring.mail.host", () -> "localhost");
		registry.add("spring.mail.port", () -> "2525");
		registry.add("spring.mail.username", () -> "test");
		registry.add("spring.mail.password", () -> "");
		registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
		registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
		registry.add("custom.app.sender.email", () -> "test@example.com");
		registry.add("stripe.secret-key", () -> "");
		registry.add("stripe.publishable-key", () -> "");
		registry.add("stripe.webhook-secret", () -> "");
		registry.add("stripe.connect.return-url", () -> "http://localhost:5173/profile");
		registry.add("stripe.connect.refresh-url", () -> "http://localhost:5173/profile");
		registry.add("stripe.platform-fee-percent", () -> "10");
	}

	@Test
	void contextLoads() {
	}

}