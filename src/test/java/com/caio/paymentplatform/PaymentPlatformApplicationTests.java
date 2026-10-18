package com.caio.paymentplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class PaymentPlatformApplicationTests {

		@Container 
		@ServiceConnection
		static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

		@Autowired
		private DataSource dataSource;

		@Test
		void shouldConnectToPostgres() throws SQLException {

			try (Connection connection = dataSource.getConnection()) {
				assertTrue(connection.isValid(2));
			}
		}
}