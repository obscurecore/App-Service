package ru.ruslan.zuulservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * Zuul - proxy, gateway between user and services
 * need to handle outside request and routing to the desired internal infrastructure services
 *
 * It start with launch pre-filters, pass request using Netty and then return response after launch post-filters
 *
 * Filters are the basis of functionality.They can be performed in different parts of the lifecycle(req-res)
 * 		cause they bear responsibility for the application's business logic
 *
 * 	pre-filter:
 * 	 Performed before routing and can be used for authentication, routing and req processing, speed limit, security from DDoS
 * 	endpoint-filter:
 *   bear for req processing
 *  post-filter:
 *   running after handle response from source and can be used for metric or amend answer
 *  error-filter:
 *   when the error occurs, during on of the other stages
 *
 *
 *  So with this approach user can communicate with gallery-service not through (http://localhost:8081)
 *  	 and directly through zuul itself. this way(http://localhost:8766/gallery) or to gallery-service
 *  	 (http://localhost:8766/users)
 *
 *  	 services don't know about each other and don't know anything about zuul. lowest coupling
 *  	 only need know the servce name
 *
 *
 */
@SpringBootApplication
//need to Eureka find him
@EnableEurekaClient
@EnableZuulProxy
public class ZuulServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZuulServiceApplication.class, args);
	}

}
