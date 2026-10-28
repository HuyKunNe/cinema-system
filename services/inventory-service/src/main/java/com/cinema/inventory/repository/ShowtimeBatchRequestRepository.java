package com.cinema.inventory.repository;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ShowtimeBatchRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShowtimeBatchRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StoredBatch> find(UUID clientRequestId) {
        return read(clientRequestId, false);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void claim(UUID clientRequestId, String requestHash) {
        jdbcTemplate.update(
                """
                INSERT INTO showtime_batch_requests (
                    client_request_id,
                    request_hash
                )
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE
                    client_request_id = client_request_id
                """,
                binary(clientRequestId),
                requestHash);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public StoredBatch findForUpdate(UUID clientRequestId) {
        return read(clientRequestId, true)
                .orElseThrow(
                        () ->
                                new InternalServerException(
                                        InventoryErrorCode.SHOWTIME_BATCH_REQUEST_INCONSISTENT));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID clientRequestId, String requestHash, String responseJson) {

        int updated =
                jdbcTemplate.update(
                        """
                        UPDATE showtime_batch_requests
                        SET response_json = ?
                        WHERE client_request_id = ?
                          AND request_hash = ?
                          AND response_json IS NULL
                        """,
                        responseJson,
                        binary(clientRequestId),
                        requestHash);

        if (updated != 1) {
            throw new InternalServerException(
                    InventoryErrorCode.SHOWTIME_BATCH_REQUEST_INCONSISTENT);
        }
    }

    private Optional<StoredBatch> read(UUID clientRequestId, boolean forUpdate) {

        String sql =
                """
                SELECT request_hash, response_json
                FROM showtime_batch_requests
                WHERE client_request_id = ?
                """;

        if (forUpdate) {
            sql += " FOR UPDATE";
        }

        return jdbcTemplate
                .query(
                        sql,
                        (resultSet, rowNumber) ->
                                new StoredBatch(
                                        resultSet.getString("request_hash"),
                                        resultSet.getString("response_json")),
                        binary(clientRequestId))
                .stream()
                .findFirst();
    }

    private static byte[] binary(UUID value) {
        return ByteBuffer.allocate(16)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits())
                .array();
    }

    public record StoredBatch(String requestHash, String responseJson) {}
}
