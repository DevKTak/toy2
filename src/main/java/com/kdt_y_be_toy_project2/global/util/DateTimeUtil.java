package com.kdt_y_be_toy_project2.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    public final static String LOCAL_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    public static String toString(LocalDateTime dateTimeObject) {
        return dateTimeObject.format(DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN));
    }

    public static LocalDateTime toLocalDateTime(String datetimeString) {
        return LocalDateTime.parse(datetimeString, DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN));
    }
}