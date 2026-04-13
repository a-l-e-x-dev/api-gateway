package com.innowise.api_gateway;

import com.innowise.api_gateway.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationOrchestrator {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.auth.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.user.url:http://localhost:8082}")
    private String userServiceUrl;

    public Mono<String> registerUser(RegistrationRequest request) {

        return webClientBuilder.build()
                .post()
                .uri(authServiceUrl + "/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)

                .flatMap(authResponse -> saveUserProfile(request)

                        .onErrorResume(error -> {
                            log.error("User Service failed! Rolling back Auth Service for email: {}", request.getEmail());
                            return rollbackAuthCredentials(request.getEmail())
                                    .then(Mono.error(new RuntimeException("Registration failed, transaction rolled back.")));
                        })
                );
    }

    private Mono<String> saveUserProfile(RegistrationRequest request) {
        return webClientBuilder.build()
                .post()
                .uri(userServiceUrl + "/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class);
    }

    private Mono<Void> rollbackAuthCredentials(String email) {
        return webClientBuilder.build()
                .delete()
                .uri(authServiceUrl + "/api/v1/auth/internal/credentials/" + email)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Rollback successful for email: {}", email))
                .doOnError(e -> log.error("CRITICAL: Rollback failed for email: {}! Manual intervention required.", email));
    }
}