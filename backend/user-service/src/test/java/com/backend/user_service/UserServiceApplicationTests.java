package com.backend.user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the application context loads successfully
	}

	@Test
	void mainMethodRuns() {
		// Verify the main method can be invoked
		UserServiceApplication.main(new String[] {});
	}
}
