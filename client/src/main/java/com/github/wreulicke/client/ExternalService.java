package com.github.wreulicke.client;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;

/**
 * ExternalService
 */
@Service
public class ExternalService {

    public CompletableFuture<AggregatedHttpResponse> get() {
        HttpClient httpClient = HttpClient.of("http://group:backend/");
        return httpClient.get("/").aggregate();
    }
}