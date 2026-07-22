package com.cinema.common.search.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchUtils {

    private SearchUtils() {

    }

    public static String normalizeKeyword(
            String keyword) {

        if (keyword == null) {

            return "";

        }

        return keyword
                .trim()
                .replaceAll("\\s+", " ");

    }

    public static String normalizeAscii(
            String value) {

        if (value == null) {

            return "";

        }

        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD);

        return normalized
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");

    }

    public static boolean isBlank(
            String value) {

        return value == null
                || value.isBlank();

    }

}
