package com.cinema.inventory.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.PricingDayGroup;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

class CinemaPricingPolicyVersionScopeTest {

    @Test
    void draftShouldAllowReplacingCinemaScope() {
        Cinema first = cinema("First");
        Cinema second = cinema("Second");

        CinemaPricingPolicyVersion revision = draft(first, second);

        assertThat(revision.getCinemaIds())
                .containsExactlyInAnyOrder(first.getId(), second.getId());

        revision.replaceDraftCinemas(Set.of(second));

        assertThat(revision.appliesTo(first.getId())).isFalse();
        assertThat(revision.appliesTo(second.getId())).isTrue();
    }

    @Test
    void publishedVersionShouldRejectScopeChanges() {
        Cinema first = cinema("First");
        Cinema second = cinema("Second");

        CinemaPricingPolicyVersion revision = draft(first, second);

        revision.publish(OffsetDateTime.parse("2026-10-26T00:00:00Z"), "scope-test-actor");

        assertThatThrownBy(() -> revision.replaceDraftCinemas(Set.of(first)))
                .isInstanceOf(ConflictException.class);

        assertThat(revision.getCinemaIds())
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void emptyScopeShouldBeRejectedWithoutChangingCurrentScope() {
        Cinema first = cinema("First");

        CinemaPricingPolicyVersion revision = draft(first);

        assertThatThrownBy(() -> revision.replaceDraftCinemas(Set.of()))
                .isInstanceOf(ValidationException.class);

        assertThat(revision.getCinemaIds()).containsExactly(first.getId());
    }

    private CinemaPricingPolicyVersion draft(Cinema... cinemas) {
        CinemaPricingPolicy policy =
                new CinemaPricingPolicy(
                        cinemas[0],
                        UUID.fromString("019a0c12-1000-7000-8000-000000000001"),
                        "a".repeat(64),
                        "b".repeat(64));

        return new CinemaPricingPolicyVersion(
                policy,
                1,
                new CinemaPricingPolicyVersion.Metadata(
                        "Shared pricing", null, PricingDayGroup.WEEKDAY),
                Set.of(cinemas));
    }

    private Cinema cinema(String name) {
        return new Cinema(name, "Test address", "Test city");
    }
}
