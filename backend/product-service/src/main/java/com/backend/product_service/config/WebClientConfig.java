package com.backend.product_service.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() throws NoSuchAlgorithmException, KeyStoreException, SSLException {

        // ✅ FIX: Create an "insecure" SSL context that trusts all certificates
        // This is safe for development with self-signed certificates
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // Trust all certificates
                .build();

        // Create HttpClient with the insecure SSL context
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));

        // Build the WebClient.Builder
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    // ✅ ADD THIS: Insecure trust manager for development
    private static class InsecureTrustManagerFactory {
        public static final io.netty.handler.ssl.util.InsecureTrustManagerFactory INSTANCE =
                (io.netty.handler.ssl.util.InsecureTrustManagerFactory) io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE;
    }
}