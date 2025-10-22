package com.backend.user_service.debug; // Adjust package name if needed

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class SslDebugRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SslDebugRunner.class);

    @Value("${server.ssl.key-store}") // Inject the property value
    private Resource keyStoreResource;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword; // Just to see if it loads

    @Value("${server.ssl.key-alias}")
    private String keyAlias; // Just to see if it loads

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("===== SSL Keystore Debug =====");
        if (keyStoreResource == null) {
            log.error("KeyStore Resource is NULL. Property 'server.ssl.key-store' might not be set correctly.");
            return;
        }

        try {
            log.info("Attempting to load keystore from property: '{}'", keyStoreResource.getDescription());
            log.info("Resolved Keystore URI: {}", keyStoreResource.getURI());
            log.info("Resolved Keystore Path (if applicable): {}", keyStoreResource.isFile() ? keyStoreResource.getFile().getAbsolutePath() : "Not a direct file path");
            log.info("Keystore exists? {}", keyStoreResource.exists());

            // Try opening the stream as a basic check
            if (keyStoreResource.exists()) {
                try (InputStream is = keyStoreResource.getInputStream()) {
                    log.info("Successfully opened InputStream for keystore.");
                } catch (Exception e) {
                    log.error("FAILED to open InputStream for keystore: {}", e.getMessage());
                }
            } else {
                log.error("Keystore resource reported as non-existent!");
            }

            log.info("Configured Keystore Password: {}", keyStorePassword != null && !keyStorePassword.isEmpty() ? "[SET]" : "[NOT SET or EMPTY]");
            log.info("Configured Keystore Alias: {}", keyAlias);

        } catch (Exception e) {
            log.error("Error accessing KeyStore Resource: {}", e.getMessage(), e);
        }
        log.info("==============================");
    }
}