package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.enums.TicketCategory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public record ReplaceCinemaPricingPolicyContentRequest(
        @NotNull @PositiveOrZero @JsonDeserialize(using = StrictLongDeserializer.class)
                Long expectedVersion,
        @NotNull @Size(max = 500) List<@NotNull @Valid RuleInput> rules) {

    public record RuleInput(
            @NotBlank @Size(max = 64) String code,
            @NotNull TicketCategory ticketCategory,
            @NotNull PricingDayType dayType,
            @NotEmpty @Size(max = 7) Set<@NotNull DayOfWeek> daysOfWeek,
            @NotNull @Min(0) @Max(1439) @JsonDeserialize(using = StrictIntegerDeserializer.class)
                    Integer fromMinute,
            @NotNull @Min(1) @Max(1440) @JsonDeserialize(using = StrictIntegerDeserializer.class)
                    Integer toMinute,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull @PositiveOrZero @JsonDeserialize(using = StrictIntegerDeserializer.class)
                    Integer priority,
            @Size(max = 64) String requiredMemberTier,
            @Size(max = 64) String requiredChannel) {}

    public static final class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {

            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return context.reportInputMismatch(
                        Integer.class, "Pricing minutes and priorities must be JSON integers");
            }

            return parser.getIntValue();
        }
    }

    public static final class StrictLongDeserializer extends JsonDeserializer<Long> {

        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {

            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return context.reportInputMismatch(
                        Long.class, "expectedVersion must be a JSON integer");
            }

            return parser.getLongValue();
        }
    }
}
