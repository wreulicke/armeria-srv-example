package com.github.wreulicke.client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RootController
 */
@RestController
public class RootController {

    private final ExternalService externalService;

    RootController(ExternalService externalService) {
        this.externalService = externalService;
    }

    @GetMapping("/")
    public CompletableFuture<String> index() {
        return externalService.get()
            .thenApply(r -> "Response: " + r.content().toString(StandardCharsets.UTF_8));
    }

}
