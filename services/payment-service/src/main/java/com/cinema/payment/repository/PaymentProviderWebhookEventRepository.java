package com.cinema.payment.repository;

import com.cinema.payment.entity.PaymentProviderWebhookEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface PaymentProviderWebhookEventRepository
        extends JpaRepository<PaymentProviderWebhookEvent, UUID> {

    long countByProviderAndProviderEventId(String provider, String providerEventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    INSERT IGNORE INTO payment_provider_webhook_events (
                        id,
                        payment_transaction_id,
                        provider,
                        provider_event_id,
                        provider_reference,
                        outcome,
                        amount,
                        currency,
                        occurred_at,
                        processed_at
                    )
                    VALUES (
                        UUID_TO_BIN(:id),
                        UUID_TO_BIN(:paymentTransactionId),
                        :provider,
                        :providerEventId,
                        :providerReference,
                        :outcome,
                        :amount,
                        :currency,
                        :occurredAt,
                        :processedAt
                    )
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("paymentTransactionId") String paymentTransactionId,
            @Param("provider") String provider,
            @Param("providerEventId") String providerEventId,
            @Param("providerReference") String providerReference,
            @Param("outcome") String outcome,
            @Param("amount") BigDecimal amount,
            @Param("currency") String currency,
            @Param("occurredAt") OffsetDateTime occurredAt,
            @Param("processedAt") OffsetDateTime processedAt);
}
