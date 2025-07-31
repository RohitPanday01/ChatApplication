package com.rohit.ChatApplication.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    public static final DateTimeFormatter FORMATTER_WITH_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.of("UTC"));

    public static String formatInstant(Instant instant) {
        return FORMATTER_WITH_MILLIS.format(instant);
    }

}
