package com.manuelr.microservices.cms.apigateway.config;

import com.manuelr.cms.commons.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;

@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final String VALIDATE_TOKEN_URI = "http://auth-server/api/v1/auth/validate_token";
    private static final String AUTH_HEADER_NAME = "X-Auth-Token";

    @Value("${authentication-dev.auth.accessTokenCookieName}")
    private String accessTokenCookieName;

    private final WebClient.Builder webClientBuilder;

    public AuthFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String encryptedJwtToken = getJwtFromCookie(exchange.getRequest());
            log.info("Api Gateway Encrypted Token ---> {}", encryptedJwtToken);
            return webClientBuilder.build()
                    .post()
                    .uri(VALIDATE_TOKEN_URI)
                    .cookie(accessTokenCookieName, encryptedJwtToken)
                    .retrieve().bodyToMono(UserDto.class)
                    .map(userDto -> {
                        exchange.getRequest()
                                .mutate()
                                .header(AUTH_HEADER_NAME, encryptedJwtToken);
                        return exchange;
                    }).flatMap(chain::filter);
        };
    }

    private String getJwtFromCookie(ServerHttpRequest request) {
        HttpCookie jwtCookie = request.getCookies().values().stream().flatMap(Collection::stream)
                .filter(httpCookie -> accessTokenCookieName.equals(httpCookie.getName())).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Missing Auth Token"));
        return jwtCookie.getValue();
    }

    public static class Config {
    }
}

