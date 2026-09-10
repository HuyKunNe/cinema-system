package com.cinema.payment.provider.webhook.model;

public enum ProviderWebhookApplicationDisposition {
    APPLIED,
    DUPLICATE,
    CONFIRMED_EXISTING,
    IGNORED_STALE
}
