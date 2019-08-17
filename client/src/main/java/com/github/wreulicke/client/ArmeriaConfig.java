package com.github.wreulicke.client;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.HttpClientBuilder;
import com.linecorp.armeria.client.endpoint.EndpointGroupRegistry;
import com.linecorp.armeria.client.endpoint.EndpointSelectionStrategy;
import com.linecorp.armeria.client.endpoint.StaticEndpointGroup;
import com.linecorp.armeria.client.endpoint.dns.DnsServiceEndpointGroup;
import com.linecorp.armeria.client.endpoint.dns.DnsServiceEndpointGroupBuilder;
import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.client.retry.Backoff;

/**
 * ArmeriaConfig
 */
@Configuration
public class ArmeriaConfig {
	
	@Bean
	public HttpClient httpClient() {
		return new HttpClientBuilder("http://group:backend/")
			.decorator(LoggingClient.newDecorator()) // TODO write LoggingDecorator like okhttp
			.build();
	}
	
	@Configuration
	@Profile("!production")
	static class DevelopmentArmeriaConfig implements InitializingBean {
		
		@Override
		public void afterPropertiesSet() throws Exception {
			StaticEndpointGroup endpointGroup = new StaticEndpointGroup(Endpoint.of("localhost", 8080));
			EndpointGroupRegistry.register("backend", endpointGroup, EndpointSelectionStrategy.WEIGHTED_ROUND_ROBIN);
		}
	}
	
	@Configuration
	@Profile("production")
	public static class ProductionArmeriaConfig implements InitializingBean {
		
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
}
