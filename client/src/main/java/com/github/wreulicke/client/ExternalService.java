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

    private final HttpClient httpClient;
    
    public ExternalService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    public CompletableFuture<AggregatedHttpResponse> get() {
        return httpClient.get("/").aggregate();
    }
}
