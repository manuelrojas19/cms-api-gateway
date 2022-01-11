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
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {
    public static final String VALIDATE_TOKEN_URI = "http://cms-auth-server/api/v1/auth/validate_token";

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
            String jwt = getJwtFromCookie(exchange.getRequest());
            if (Objects.isNull(jwt)) {
                throw new RuntimeException("Missing Auth Token");
            }
            log.info("Token: {}", jwt);

            return webClientBuilder.build()
                    .post()
                    .uri(VALIDATE_TOKEN_URI)
                    .cookie(accessTokenCookieName, jwt)
                    .retrieve().bodyToMono(UserDto.class)
                    .map(userDto -> {
                        System.out.println(exchange.getRequest().getHeaders());
                        exchange.getRequest()
                                .mutate()
                                .header("X-Auth-UserId", String.valueOf(userDto.getId()))
                                .header("X-Auth-UserRole", String.valueOf(userDto.getRole()));
                        return exchange;
                    }).flatMap(chain::filter);
        };
    }

    private String getJwtFromCookie(ServerHttpRequest request) {
        Optional<HttpCookie> jwtCookie = request.getCookies().values().stream().flatMap(Collection::stream)
                .filter(httpCookie -> accessTokenCookieName.equals(httpCookie.getName())).findAny();
        if (jwtCookie.isEmpty())
            return null;
        return jwtCookie.get().getValue();
    }

    public static class Config {

    }
}

