package com.cinema.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;

@ActiveProfiles({
        "test",
        "native"
})
@SpringBootTest(classes = ConfigServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnBookingServiceConfiguration() {

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/booking-service/default",
                JsonNode.class);

        assertThat(
                response.getStatusCode())
                .isEqualTo(
                        HttpStatus.OK);

        assertThat(
                response.getBody())
                .isNotNull();

        assertThat(
                response.getBody()
                        .get("name")
                        .asText())
                .isEqualTo(
                        "booking-service");

        assertThat(
                response.getBody()
                        .get("propertySources")
                        .isArray())
                .isTrue();

        assertThat(
                response.getBody()
                        .get("propertySources")
                        .size())
                .isGreaterThanOrEqualTo(
                        1);

    }

    @Test
    void shouldExposeHealthEndpoint() {

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/actuator/health",
                JsonNode.class);

        assertThat(
                response.getStatusCode())
                .isEqualTo(
                        HttpStatus.OK);

        assertThat(
                response.getBody())
                .isNotNull();

        assertThat(
                response.getBody()
                        .get("status")
                        .asText())
                .isEqualTo(
                        "UP");

    }

}
