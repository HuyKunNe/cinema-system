package com.cinema.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(classes = DiscoveryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaDashboardEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldExposeEurekaDashboard() {

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/",
                String.class);

        assertThat(
                response.getStatusCode())
                .isEqualTo(
                        HttpStatus.OK);

        assertThat(
                response.getBody())
                .isNotBlank();

        assertThat(
                response.getBody())
                .containsIgnoringCase(
                        "eureka");

    }

    @Test
    void shouldExposeHealthEndpoint() {

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health",
                String.class);

        assertThat(
                response.getStatusCode())
                .isEqualTo(
                        HttpStatus.OK);

        assertThat(
                response.getBody())
                .contains(
                        "\"status\":\"UP\"");

    }

}
