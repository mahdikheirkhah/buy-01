package com.backend.dummy_data.generator;

import com.backend.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DummyDataGenerator {

    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.dummy-data.enabled:true}")
    private boolean enabled;

    @Value("${app.api-gateway.url:https://localhost:8443}")
    private String gatewayUrl;

    private String adminTokenCookie;

    // Store Seller details (for login later)
    private List<Map<String, Object>> createdSellers = new ArrayList<>();

    @PostConstruct
    public void generate() {
        if (!enabled) {
            System.out.println("Dummy data generation is disabled.");
            return;
        }

        System.out.println("Starting dummy data generation...");

        try {
            // 1. Login as Admin (Only needed for GET /api/users/email and registration)
            adminTokenCookie = login("admin@admin.com", "admin123");
            System.out.println("Logged in as admin.");

            // 2. Create Sellers and Clients
            createSellers();
            createClients();
            System.out.println("Created " + createdSellers.size() + " sellers and clients.");

            // 3. Login as each Seller and create products
            for (Map<String, Object> seller : createdSellers) {
                loginAndCreateProducts(seller);
            }

            System.out.println("Dummy data generated successfully!");

        } catch (Exception e) {
            System.err.println("Failed to generate dummy data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String login(String email, String password) {
        Map<String, String> login = Map.of("email", email, "password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(login, headers);

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                gatewayUrl + "/api/auth/login", entity, Map.class);

        return extractJwtFromCookies(resp.getHeaders());
    }

    private void createSellers() throws Exception {
        List<Map<String, Object>> sellers = List.of(
                Map.of("firstName", "Nordic", "lastName", "Designs", "email", "seller1@shop.no", "password", "pass123", "role", "SELLER"),
                Map.of("firstName", "Scandi", "lastName", "Living", "email", "seller2@scandi.se", "password", "pass123", "role", "SELLER"),
                Map.of("firstName", "Fjall", "lastName", "Home", "email", "seller3@fjall.dk", "password", "pass123", "role", "SELLER"),
                Map.of("firstName", "Aurora", "lastName", "Lights", "email", "seller4@aurora.fi", "password", "pass123", "role", "SELLER"),
                Map.of("firstName", "Viking", "lastName", "Craft", "email", "seller5@viking.is", "password", "pass123", "role", "SELLER")
        );

        for (Map<String, Object> s : sellers) {
            try {
                registerUser(s);
                createdSellers.add(s); // Store credentials for later login
            } catch (Exception e) {
                System.err.println("Could not register seller " + s.get("email") + ": " + e.getMessage());
            }
        }
    }

    private void createClients() throws Exception {
        List<Map<String, Object>> clients = List.of(
                Map.of("firstName", "Lars", "lastName", "Hansen", "email", "lars@client.dk", "password", "pass123", "role", "CLIENT"),
                Map.of("firstName", "Ingrid", "lastName", "Svensson", "email", "ingrid@client.se", "password", "pass123", "role", "CLIENT"),
                Map.of("firstName", "Bjorn", "lastName", "Olsen", "email", "bjorn@client.no", "password", "pass123", "role", "CLIENT"),
                Map.of("firstName", "Freya", "lastName", "Nielsen", "email", "freya@client.dk", "password", "pass123", "role", "CLIENT"),
                Map.of("firstName", "Erik", "lastName", "Larsson", "email", "erik@client.se", "password", "pass123", "role", "CLIENT")
        );

        for (Map<String, Object> c : clients) {
            try {
                registerUser(c);
            } catch (Exception e) {
                System.err.println("Could not register client " + c.get("email") + ": " + e.getMessage());
            }
        }
    }

    private ResponseEntity<Map> registerUser(Map<String, Object> userDto) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        HttpHeaders dtoHeaders = new HttpHeaders();
        dtoHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> dtoEntity = new HttpEntity<>(objectMapper.writeValueAsString(userDto), dtoHeaders);
        body.add("userDto", dtoEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // Admin token is required to register other users if registration is restricted
        headers.set("Cookie", adminTokenCookie);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.postForEntity(gatewayUrl + "/api/auth/register", requestEntity, Map.class);
    }

    private void loginAndCreateProducts(Map<String, Object> seller) throws Exception {
        String sellerEmail = (String) seller.get("email");
        String sellerPassword = (String) seller.get("password");

        System.out.println(" -- Logging in as seller: " + sellerEmail);
        String sellerTokenCookie = login(sellerEmail, sellerPassword);

        List<String> names = List.of(
                "Wool Blanket", "Ceramic Mug", "Wall Clock", "Cotton Towel", "Wood Tray",
                "LED Candle", "Glass Vase", "Linen Sheets", "Bamboo Cutlery", "Marble Coaster"
        );

        Random rand = new Random();
        for (String name : names) {
            Map<String, Object> product = Map.of(
                    "name", name + " - " + (rand.nextInt(1000) + 100),
                    "description", "Premium quality " + name.toLowerCase() + " from Scandinavia.",
                    "price", 29.99 + rand.nextDouble() * 120,
                    "quantity", 10 + rand.nextInt(40)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // *** Use the Seller's specific token here ***
            headers.set("Cookie", sellerTokenCookie);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(product, headers);
            try {
                ResponseEntity<Map> resp = restTemplate.postForEntity(
                        gatewayUrl + "/api/products", entity, Map.class);

                String productId = (String) resp.getBody().get("id"); // Assuming the service returns "id"
                if(productId == null) productId = (String) resp.getBody().get("productId");

                if (productId != null) {
                    System.out.println("    -> Created product: " + product.get("name") + " (ID: " + productId + ")");
                    uploadRandomImages(productId, sellerTokenCookie, 1 + rand.nextInt(3));
                }
            } catch (Exception e) {
                // This might fail if the token doesn't have SELLER role or if the DTO is missing fields.
                System.err.println("    -> Failed to create product for " + sellerEmail + ": " + e.getMessage());
            }
        }
    }

    private void uploadRandomImages(String productId, String tokenCookie, int count) {
        for (int i = 0; i < count; i++) {
            try {
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("productId", productId);

                // Use a dummy byte array acting as a file (less susceptible to network issues)
                ByteArrayResource dummyFile = new ByteArrayResource(new byte[100]) {
                    @Override
                    public String getFilename() {
                        return "dummy-image-" + UUID.randomUUID() + ".jpg";
                    }
                };
                body.add("file", dummyFile);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                // *** Use the Seller's specific token for media upload too ***
                headers.set("Cookie", tokenCookie);

                HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
                // Corrected endpoint URL assumption: /api/media/images is common for uploads
                restTemplate.postForEntity(gatewayUrl + "/api/products/create/images", entity, String.class);

            } catch (Exception e) {
                System.err.println("    -> Failed to upload image for product " + productId);
            }
        }
    }

    private String extractJwtFromCookies(HttpHeaders headers) {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null) throw new RuntimeException("No Set-Cookie header in response.");

        return cookies.stream()
                .flatMap(cookie -> Arrays.stream(cookie.split(";")))
                .map(String::trim)
                .filter(s -> s.startsWith("jwt="))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("JWT cookie not found in response."));
    }

    // Removed unused getUserIdByEmail method
}