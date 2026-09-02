package com.dongah.fastcharger.basefunction;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class DateTimeConvert {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String convertY2ToY4(String value) {
        try {
            //KST --> UTC
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyMMdd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            // 1. yyMMdd → LocalDate
            LocalDate localDate = LocalDate.parse(value, inputFormatter);

//            // 2. KST 기준 00:00:00
//            ZonedDateTime kstDateTime = localDate.atStartOfDay(ZoneId.of("Asia/Seoul"));
//            // 3. UTC 변환
//            ZonedDateTime utcDateTime = kstDateTime.withZoneSameInstant(ZoneOffset.UTC);
//            // 4. yyyyMMdd 반환
//            return utcDateTime.format(outputFormatter);
            return localDate.format(outputFormatter);
        } catch (Exception e) {
            return value;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String convertY4ToY2(String value) {
        try {
            DateTimeFormatter input = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter output = DateTimeFormatter.ofPattern("yyMMdd");
            LocalDate date = LocalDate.parse(value, input);
            return date.format(output);
        } catch (Exception e) {
            return value;
        }
    }

    @SuppressLint("SimpleDateFormat")
    public String dateTimeCovertY4ToY2(String value) {
        DateFormat formatter;
        Date date = null;
        formatter = new SimpleDateFormat("yyyyMMdd");
        try {
            date = formatter.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        formatter = new SimpleDateFormat("yyMMdd");
        return formatter.format(date);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String dateTimeCovertZoneDate(String value) {
        // yyyyMMddHHmmss => yyyy-MM-dd'T'HH:mm:ss'Z'
        final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value, INPUT_FORMATTER);
            return localDateTime.format(OUTPUT_FORMATTER);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }


    }
}
