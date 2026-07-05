package com.sylvester.bankapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class BankAppApplicationTests {

	@Test
	void applicationClassHasSpringBootConfiguration() {
		assertThat(BankAppApplication.class).hasAnnotation(SpringBootApplication.class);
		assertThat(BankAppApplication.class).hasAnnotation(EnableAsync.class);
	}

}
