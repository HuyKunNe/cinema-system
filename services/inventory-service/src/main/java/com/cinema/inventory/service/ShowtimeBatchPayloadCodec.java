package com.cinema.inventory.service;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

@Component
public class ShowtimeBatchPayloadCodec {

    private static final ObjectMapper FINGERPRINT_MAPPER = new ObjectMapper();

    private static final TypeReference<List<ShowtimeResponse>> RESPONSE_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper snapshotMapper;

    public ShowtimeBatchPayloadCodec(ObjectMapper objectMapper) {
        this.snapshotMapper =
                objectMapper
                        .copy()
                        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    public String fingerprint(List<CreateShowtimeRequest> items) {
        List<List<Object>> rows =
                items.stream()
                        .map(
                                item ->
                                        Arrays.<Object>asList(
                                                item.movieId(),
                                                item.roomId(),
                                                instant(item.startsAt()),
                                                instant(item.endsAt()),
                                                item.roomLayoutId(),
                                                item.movieScreeningVersionId(),
                                                item.breakMinutes(),
                                                item.pricingPolicyVersionId()))
                        .toList();

        try {
            byte[] payload =
                    FINGERPRINT_MAPPER.writeValueAsBytes(List.of("showtime-batch-v1", rows));

            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));

        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not fingerprint showtime batch", exception);
        }
    }

    public String encode(List<ShowtimeResponse> responses) {
        try {
            return snapshotMapper.writeValueAsString(responses);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.SHOWTIME_BATCH_REQUEST_INCONSISTENT, exception);
        }
    }

    public List<ShowtimeResponse> decode(String responseJson) {
        if (responseJson == null) {
            throw inconsistent();
        }

        try {
            List<ShowtimeResponse> responses =
                    snapshotMapper.readValue(responseJson, RESPONSE_TYPE);

            if (responses == null || responses.isEmpty()) {
                throw inconsistent();
            }

            return List.copyOf(responses);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.SHOWTIME_BATCH_REQUEST_INCONSISTENT, exception);
        }
    }

    private static String instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant().toString();
    }

    private static InternalServerException inconsistent() {
        return new InternalServerException(InventoryErrorCode.SHOWTIME_BATCH_REQUEST_INCONSISTENT);
    }
}
