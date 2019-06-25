package com.github.wreulicke.client;

import com.linecorp.armeria.client.endpoint.EndpointGroupRegistry;
import com.linecorp.armeria.client.endpoint.EndpointSelectionStrategy;
import com.linecorp.armeria.client.endpoint.dns.DnsServiceEndpointGroup;
import com.linecorp.armeria.client.endpoint.dns.DnsServiceEndpointGroupBuilder;
import com.linecorp.armeria.client.retry.Backoff;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * ArmetriaConfig
 */
@Configuration
public class ArmetriaConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        DnsServiceEndpointGroup group =
        new DnsServiceEndpointGroupBuilder("backend.internal.local")
                // Custom backoff strategy.
                .backoff(Backoff.exponential(1000, 16000).withJitter(0.3))
                .build();
 
        // Wait until the initial DNS queries are finished.
        group.awaitInitialEndpoints();
        EndpointGroupRegistry.register("backend", group, EndpointSelectionStrategy.WEIGHTED_ROUND_ROBIN);
    }
}