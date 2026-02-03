package com.opensearchloadtester.common.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeFormatter {

  private static final DateTimeFormatter UTC_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

  public static final DateTimeFormatter ISO_LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  public static String formatEpochMillisToUtcString(long epochMillis) {
    Instant instant = Instant.ofEpochMilli(epochMillis);
    return UTC_FORMATTER.format(instant);
  }
}
