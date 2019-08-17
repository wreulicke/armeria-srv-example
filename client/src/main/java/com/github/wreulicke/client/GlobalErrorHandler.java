package com.github.wreulicke.client;

import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.CompletionException;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import org.slf4j.LoggerFactory;

@Component
public class GlobalErrorHandler extends DefaultErrorWebExceptionHandler {
	
	/**
	 * Create a new {@code DefaultErrorWebExceptionHandler} instance.
	 * @param resourceProperties the resources configuration properties
	 * @param applicationContext the current application context
	 */
	public GlobalErrorHandler(ResourceProperties resourceProperties,
		ApplicationContext applicationContext,
		ServerCodecConfigurer serverCodecConfigurer) {
		super(new DefaultErrorAttributes(), resourceProperties, new ErrorProperties(), applicationContext);
		setMessageWriters(serverCodecConfigurer.getWriters());
		setMessageReaders(serverCodecConfigurer.getReaders());
	}
	
	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		return RouterFunctions.route(
			request -> {
				Throwable error = errorAttributes.getError(request);
				if (error instanceof CompletionException && error.getCause() != null) {
					error = error.getCause();
				}
				return error instanceof SocketException;
			},
			request ->
			{
				Map<String, Object> errorAttribute = getErrorAttributes(request, false);
				errorAttribute.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
				errorAttribute.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
				return
					ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
						.contentType(MediaType.APPLICATION_JSON_UTF8)
						.body(BodyInserters.fromObject(errorAttribute));
			});
	}
}
