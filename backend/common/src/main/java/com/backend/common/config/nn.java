//package com.backend.common.config;
//
//import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
//import org.apache.hc.client5.http.impl.classic.HttpClients;
//import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
//import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
//import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
//import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
//import org.apache.hc.core5.http.config.Registry;
//import org.apache.hc.core5.http.config.RegistryBuilder;
//import org.apache.hc.core5.ssl.SSLContexts;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.web.client.RestTemplate;
//
//import javax.net.ssl.SSLContext;
//import java.io.InputStream;
//import java.security.KeyStore;
//
//@Configuration
//public class SslConfig {
//
//    @Bean
//    public RestTemplate sslRestTemplate() throws Exception {
//        // Load truststore (for verifying other services' certificates)
//        KeyStore trustStore = KeyStore.getInstance("PKCS12");
//        try (InputStream trustStoreStream = getClass().getClassLoader().getResourceAsStream("ca-truststore.p12")) {
//            if (trustStoreStream == null) {
//                throw new IllegalStateException("Truststore file not found in classpath: ca-truststore.p12");
//            }
//            trustStore.load(trustStoreStream, "changeit".toCharArray());
//        }
//
//        // Create SSL context with truststore
//        SSLContext sslContext = SSLContexts.custom()
//                .loadTrustMaterial(trustStore, null)
//                .build();
//
//        // Create SSL socket factory
//        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
//
//        // Register both HTTP and HTTPS
//        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
//                .register("https", sslSocketFactory)
//                .register("http", PlainConnectionSocketFactory.getSocketFactory())
//                .build();
//
//        // Create connection manager
//        PoolingHttpClientConnectionManager connectionManager =
//                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
//        connectionManager.setMaxTotal(100);
//        connectionManager.setDefaultMaxPerRoute(20);
//
//        // Create HTTP client
//        CloseableHttpClient httpClient = HttpClients.custom()
//                .setConnectionManager(connectionManager)
//                .evictExpiredConnections()
//                .build();
//
//        // Create request factory
//        HttpComponentsClientHttpRequestFactory requestFactory =
//                new HttpComponentsClientHttpRequestFactory(httpClient);
//        requestFactory.setConnectTimeout(30000);
//        requestFactory.setConnectionRequestTimeout(30000);
//
//        return new RestTemplate(requestFactory);
//    }
//}