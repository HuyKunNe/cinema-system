package com.cinema.common.api.screening;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum ScreeningFormat {
    STANDARD_2D("STANDARD", "2D"),
    STANDARD_3D("STANDARD", "3D"),
    PREMIUM_CINEMA_2D("PREMIUM_CINEMA", "2D"),
    PREMIUM_CINEMA_3D("PREMIUM_CINEMA", "3D"),
    FOUR_DX_2D("4DX", "2D"),
    FOUR_DX_3D("4DX", "3D"),
    IMAX_2D("IMAX", "2D"),
    IMAX_3D("IMAX", "3D");

    private final String experienceType;
    private final String contentFormat;

    ScreeningFormat(String experienceType, String contentFormat) {
        this.experienceType = experienceType;
        this.contentFormat = contentFormat;
    }

    public ScreeningFormatOption toOption() {
        return new ScreeningFormatOption(
                this,
                experienceType,
                contentFormat);
    }

    public static List<ScreeningFormatOption> options(
            Set<ScreeningFormat> formats) {

        return formats.stream()
                .sorted()
                .map(ScreeningFormat::toOption)
                .toList();
    }

    public static List<ScreeningFormatOption> allOptions() {
        return Arrays.stream(values())
                .map(ScreeningFormat::toOption)
                .toList();
    }

    public record ScreeningFormatOption(
            ScreeningFormat code,
            String experienceType,
            String contentFormat) {}
}
