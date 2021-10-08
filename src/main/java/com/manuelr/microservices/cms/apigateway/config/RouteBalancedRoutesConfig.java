package com.manuelr.microservices.cms.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteBalancedRoutesConfig {

    @Bean
    public RouteLocator loadBalancedRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("employees-service",
                        r -> r.path("/api/v1/employees*", "/api/v1/employees/*")
                                .uri("lb://employees-service"))
                .route("commissions-service",
                        r -> r.path("/api/v1/commissions*", "/api/v1/commissions/*",
                                        "/api/v1/employees/*/commissions*")
                                .uri("lb://commissions-service"))
                .build();
    }

}
