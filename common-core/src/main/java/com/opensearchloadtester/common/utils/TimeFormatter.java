package com.opensearchloadtester.common.utils;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class TimeFormatter {

    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    public static String formatEpochMillisToUtcString(long epochMillis) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        return UTC_FORMATTER.format(instant);
    }
}
