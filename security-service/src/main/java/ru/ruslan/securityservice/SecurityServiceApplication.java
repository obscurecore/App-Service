package ru.ruslan.securityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * What is the principle authentication
 * 1) User send request to get token, transmitting its credentials
 * 2) Server check credentials and if all right return token
 * 3) On every request user must provide token and server will check validation
 *
 * can verify tokens on Zuul's level and let the authentication serve verify credentials and issue tokens
 * and will be blocked all request if they don't authenticated (except request to get token)
 */
@SpringBootApplication
@EnableEurekaClient
public class SecurityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityServiceApplication.class, args);
	}

}
