// dummy-data/src/main/java/com/backend/dummydata/service/DummyDataGenerator.java
package com.backend.dummy_data.generator.;

import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DummyDataGenerator {

    private final RestTemplate restTemplate = new RestTemplate();
    private final JwtUtil jwtUtil;

    @Value("${app.dummy-data.enabled:true}")
    private boolean enabled;

    @Value("${app.api-gateway.url:https://localhost:8443}")
    private String gatewayUrl;

    private String adminToken;

    @PostConstruct
    public void generate() throws Exception {
        if (!enabled) return;

        System.out.println("Generating dummy data...");

        // 1. Login as admin (or create one)
        adminToken = loginAdmin();

        // 2. Clear all data
        clearAllData();

        // 3. Create sellers
        List<String> sellerIds = createSellers();

        // 4. Create clients
        createClients();

        // 5. Create products + images
        for (String sellerId : sellerIds) {
            createProductsForSeller(sellerId);
        }

        System.out.println("Dummy data generated successfully!");
    }

    private String loginAdmin() {
        // Assume you have an admin user: admin@admin.com / admin123
        Map<String, String> login = Map.of("email", "admin@admin.com", "password", "admin123");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(login, headers(MediaType.APPLICATION_JSON));
        ResponseEntity<Map> resp = restTemplate.postForEntity(gatewayUrl + "/api/auth/login", entity, Map.class);
        return extractJwtFromCookies(resp.getHeaders());
    }

    private void clearAllData() {
        // Call DELETE endpoints or use MongoDB drop via a special endpoint
        deleteAll("/api/users");
        deleteAll("/api/products");
        deleteAll("/api/media/images");
    }

    private void deleteAll(String path) {
        HttpEntity<?> entity = new HttpEntity<>(headers(MediaType.APPLICATION_JSON, adminToken));
        try {
            restTemplate.exchange(gatewayUrl + path, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            System.out.println("No data to delete at " + path);
        }
    }

    private List<String> createSellers() {
        List<Map<String, Object>> sellers = Arrays.asList(
                Map.of("name", "Nordic Designs AS", "email", "contact@nordic.no", "password", "seller123", "role", "SELLER"),
                Map.of("name", "Scandi Living AB", "email", "hello@scandiliving.se", "password", "seller123", "role", "SELLER"),
                Map.of("name", "Fjäll Home", "email", "info@fjallhome.dk", "password", "seller123", "role", "SELLER"),
                Map.of("name", "Aurora Lights Co", "email", "sales@auroralights.fi", "password", "seller123", "role", "SELLER"),
                Map.of("name", "Viking Craft", "email", "shop@vikingcraft.is", "password", "seller123", "role", "SELLER")
        );

        List<String> ids = new ArrayList<>();
        for (Map<String, Object> s : sellers) {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(s, headers(MediaType.APPLICATION_JSON, adminToken));
            ResponseEntity<Map> resp = restTemplate.postForEntity(gatewayUrl + "/api/users/register", entity, Map.class);
            ids.add((String) ((Map) resp.getBody()).get("id"));
        }
        return ids;
    }

    private void createClients() {
        List<Map<String, Object>> clients = Arrays.asList(
                Map.of("firstName", "Lars", "lastName", "Hansen", "email", "lars.h@dk.dk", "password", "client123"),
                Map.of("firstName", "Ingrid", "lastName", "Svensson", "email", "ingrid.s@se.se", "password", "client123"),
                Map.of("firstName", "Bjørn", "lastName", "Olsen", "email", "bjorn.o@no.no", "password", "client123"),
                Map.of("firstName", "Freya", "lastName", "Nielsen", "email", "freya.n@dk.dk", "password", "client123"),
                Map.of("firstName", "Erik", "lastName", "Larsson", "email", "erik.l@se.se", "password", "client123"),
                Map.of("firstName", "Astrid", "lastName", "Berg", "email", "astrid.b@no.no", "password", "client123"),
                Map.of("firstName", "Nils", "lastName", "Kristensen", "email", "nils.k@dk.dk", "password", "client123"),
                Map.of("firstName", "Sofia", "lastName", "Pedersen", "email", "sofia.p@se.se", "password", "client123"),
                Map.of("firstName", "Anders", "lastName", "Jensen", "email", "anders.j@no.no", "password", "client123"),
                Map.of("firstName", "Elin", "lastName", "Madsen", "email", "elin.m@dk.dk", "password", "client123")
        );

        for (Map<String, Object> c : clients) {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(c, headers(MediaType.APPLICATION_JSON, adminToken));
            restTemplate.postForEntity(gatewayUrl + "/api/users/register", entity, Map.class);
        }
    }

    private void createProductsForSeller(String sellerId) {
        List<String> names = Arrays.asList(
                "Handwoven Wool Blanket", "Ceramic Coffee Mug Set", "Minimalist Wall Clock", "Organic Cotton Towels",
                "Scandinavian Wood Tray", "LED Candle Lights", "Glass Vase Collection", "Linen Bedding Set",
                "Bamboo Kitchen Utensils", "Marble Coasters"
        );

        Random rand = new Random();
        for (String name : names) {
            Map<String, Object> product = Map.of(
                    "name", name,
                    "description", "High-quality " + name.toLowerCase() + " from Scandinavia.",
                    "price", 19.99 + rand.nextDouble() * 80,
                    "quantity", 5 + rand.nextInt(50),
                    "sellerId", sellerId
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(product, headers(MediaType.APPLICATION_JSON, adminToken));
            ResponseEntity<Map> resp = restTemplate.postForEntity(gatewayUrl + "/api/products", entity, Map.class);
            String productId = (String) ((Map) resp.getBody()).get("id");

            // Upload 8–15 images
            int imgCount = 8 + rand.nextInt(8);
            for (int i = 0; i < imgCount; i++) {
                uploadImage(productId, i);
            }
        }
    }

    private void uploadImage(String productId, int index) {
        String imageUrl = "https://picsum.photos/600/800?random=" + (1000 + index);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("productId", productId);
        body.add("file", new org.springframework.core.io.UrlResource(imageUrl));

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers(MediaType.MULTIPART_FORM_DATA, adminToken));
        restTemplate.postForEntity(gatewayUrl + "/api/media/images", entity, String.class);
    }

    private HttpHeaders headers(MediaType contentType) {
        return headers(contentType, null);
    }

    private HttpHeaders headers(MediaType contentType, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(contentType);
        if (token != null) {
            h.setBearerAuth(token);
        }
        return h;
    }

    private String extractJwtFromCookies(HttpHeaders headers) {
        return headers.getFirst(HttpHeaders.SET_COOKIE)
                .split("jwt=")[1]
                .split(";")[0];
    }
}