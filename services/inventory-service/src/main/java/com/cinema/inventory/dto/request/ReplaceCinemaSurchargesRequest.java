package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.SurchargeTarget;
import com.cinema.inventory.enums.SurchargeUnit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public record ReplaceCinemaSurchargesRequest(
        @NotNull
                @PositiveOrZero
                @JsonDeserialize(
                        using =
                                ReplaceCinemaPricingPolicyContentRequest.StrictLongDeserializer
                                        .class)
                Long expectedVersion,
        @NotNull @Size(max = 500) List<@NotNull @Valid SurchargeInput> surcharges) {

    public record SurchargeInput(
            @NotBlank @Size(max = 64) String code,
            @NotNull SurchargeTarget target,
            @NotBlank @Size(max = 64) String targetCode,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull SurchargeUnit unit,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean stackable,
            @NotNull
                    @PositiveOrZero
                    @JsonDeserialize(
                            using =
                                    ReplaceCinemaPricingPolicyContentRequest
                                            .StrictIntegerDeserializer.class)
                    Integer priority) {}

    public static final class StrictBooleanDeserializer extends JsonDeserializer<Boolean> {

        @Override
        public Boolean deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {

            if (parser.hasToken(JsonToken.VALUE_TRUE)) {
                return Boolean.TRUE;
            }

            if (parser.hasToken(JsonToken.VALUE_FALSE)) {
                return Boolean.FALSE;
            }

            return context.reportInputMismatch(Boolean.class, "stackable must be a JSON boolean");
        }
    }
}
