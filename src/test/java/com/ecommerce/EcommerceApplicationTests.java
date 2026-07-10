package com.ecommerce;

import com.ecommerce.modules.identity.application.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = com.ecommerce.bootstrap.EcommerceApplication.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
class EcommerceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("ecommerce")
            .withPassword("ecommerce123");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static GenericContainer<?> rabbitmq = new GenericContainer<>(DockerImageName.parse("rabbitmq:3-management-alpine"))
            .withExposedPorts(5672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getFirstMappedPort);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private static String accessToken;
    private static String tenantId;
    private static final String TEST_EMAIL = "test-" + System.currentTimeMillis() + "@ecommerce.com";

    @Test
    @Order(1)
    @DisplayName("Should register a new user")
    void registerUser() {
        RegisterRequest request = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password("secure123")
                .confirmPassword("secure123")
                .firstName("Jane")
                .lastName("Seller")
                .build();

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @Order(2)
    @DisplayName("Should login and obtain JWT tokens")
    void login() {
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password("secure123")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();

        accessToken = response.getBody().getAccessToken();
    }

    @Test
    @Order(3)
    @DisplayName("Should register a tenant")
    void registerTenant() {
        assertThat(accessToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        String slug = "test-store-" + System.currentTimeMillis();
        Map<String, String> body = Map.of(
                "name", "Test Store",
                "slug", slug,
                "description", "Test store description"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tenants",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("name")).isEqualTo("Test Store");
        assertThat(response.getBody().get("status")).isEqualTo("TRIAL");

        tenantId = (String) response.getBody().get("id");
    }

    @Test
    @Order(4)
    @DisplayName("Should create a product")
    void createProduct() {
        assertThat(accessToken).isNotBlank();
        assertThat(tenantId).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("X-Tenant-ID", tenantId);

        String slug = "test-product-" + System.currentTimeMillis();
        Map<String, Object> body = Map.of(
                "name", "Test Product",
                "slug", slug,
                "description", "Test product desc",
                "price", 49.99,
                "inventory", 100
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/catalog/products",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("name")).isEqualTo("Test Product");
        assertThat(response.getBody().get("status")).isEqualTo("DRAFT");
    }

    @Test
    @Order(5)
    @DisplayName("Should reject mismatched passwords")
    void registerWithMismatchedPasswords() {
        RegisterRequest request = RegisterRequest.builder()
                .email("bad-" + System.currentTimeMillis() + "@test.com")
                .password("secure123")
                .confirmPassword("different")
                .firstName("Bad")
                .lastName("User")
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(6)
    @DisplayName("Should reject duplicate email")
    void registerDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password("secure123")
                .confirmPassword("secure123")
                .firstName("Duplicate")
                .lastName("User")
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(7)
    @DisplayName("Should reject unauthenticated access")
    void unauthenticatedAccess() {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/tenants/me",
                    HttpMethod.GET,
                    null,
                    Map.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}
