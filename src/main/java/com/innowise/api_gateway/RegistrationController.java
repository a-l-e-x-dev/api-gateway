package com.innowise.api_gateway;

import com.innowise.api_gateway.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationOrchestrator orchestrator;

    @PostMapping("/register")
    public Mono<ResponseEntity<String>> register(@RequestBody RegistrationRequest request) {
        return orchestrator.registerUser(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body("User successfully registered!"))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage())));
    }
}