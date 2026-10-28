package com.cinema.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.exception.exception.BusinessException;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.controller.advice.ShowtimeBatchExceptionHandler;
import com.cinema.inventory.dto.request.CreateShowtimeBatchRequest;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.exception.MovieCatalogUnavailableException;
import com.cinema.inventory.exception.ShowtimeBatchItemException;
import com.cinema.inventory.service.ShowtimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

class ShowtimeBatchExceptionHandlerTest {

    @ParameterizedTest
    @MethodSource("businessFailures")
    void shouldPreserveOriginalErrorAndIdentifyRequestItem(
            BusinessException original, int expectedStatus) throws Exception {

        ShowtimeService service = mock(ShowtimeService.class);
        GlobalExceptionHandler globalHandler = new GlobalExceptionHandler();

        ShowtimeBatchExceptionHandler batchHandler =
                new ShowtimeBatchExceptionHandler(globalHandler);

        ObjectMapper mapper =
                new ObjectMapper()
                        .findAndRegisterModules()
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MockMvc mockMvc =
                MockMvcBuilders.standaloneSetup(new ShowtimeController(service))
                        .setControllerAdvice(batchHandler, globalHandler)
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                        .build();

        CreateShowtimeBatchRequest request =
                new CreateShowtimeBatchRequest(
                        UUID.randomUUID(),
                        List.of(
                                item("2099-01-01T03:00:00Z", "2099-01-01T05:00:00Z"),
                                item("2099-01-01T05:15:00Z", "2099-01-01T07:15:00Z")));

        when(service.createBatch(any(CreateShowtimeBatchRequest.class)))
                .thenThrow(new ShowtimeBatchItemException(1, original));

        mockMvc.perform(
                        post("/api/v1/showtimes/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(original.getErrorCode().code()))
                .andExpect(jsonPath("$.error.message").value(original.getErrorCode().message()))
                .andExpect(
                        jsonPath("$.error.category")
                                .value(original.getErrorCode().category().name()))
                .andExpect(jsonPath("$.error.details[0].field").value("showtimes[1]"))
                .andExpect(
                        jsonPath("$.error.details[0].message")
                                .value(original.getErrorCode().message()));
    }

    private static Stream<Arguments> businessFailures() {
        return Stream.of(
                Arguments.of(
                        new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_BATCH), 400),
                Arguments.of(new NotFoundException(InventoryErrorCode.MOVIE_NOT_FOUND), 404),
                Arguments.of(new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP), 409),
                Arguments.of(new MovieCatalogUnavailableException(), 503));
    }

    private static CreateShowtimeRequest item(String startsAt, String endsAt) {
        return new CreateShowtimeRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.parse(startsAt),
                OffsetDateTime.parse(endsAt),
                null,
                UUID.randomUUID(),
                15,
                null);
    }
}
