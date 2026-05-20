package com.dongah.fastcharger.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTransformation {

    private static final Logger logger = LoggerFactory.getLogger(DataTransformation.class);


    public String hexToString(String hex) {
        if (hex == null || hex.isEmpty()) return "";

        // 홀수 길이 방지
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex 문자열 길이는 반드시 짝수여야 합니다.");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }

        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
