package com.backend.user_service.config;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLException;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;

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

        @Bean
        @LoadBalanced
        public RestTemplate restTemplate() {
                return new RestTemplate();
        }

        // ✅ ADD THIS: Insecure trust manager for development
        private static class InsecureTrustManagerFactory {
                public static final io.netty.handler.ssl.util.InsecureTrustManagerFactory INSTANCE = (io.netty.handler.ssl.util.InsecureTrustManagerFactory) io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE;
        }
}