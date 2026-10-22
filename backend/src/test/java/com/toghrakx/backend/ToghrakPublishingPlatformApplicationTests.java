package com.toghrakx.backend;

import org.junit.jupiter.api.Test;

@IntegrationTest
class ToghrakPublishingPlatformApplicationTests {

	// Starting the context runs every Flyway migration on an empty database and then
	// Hibernate validates the schema against the entities, the same as a fresh production deploy.
	@Test
	void contextLoads() {
	}

}
