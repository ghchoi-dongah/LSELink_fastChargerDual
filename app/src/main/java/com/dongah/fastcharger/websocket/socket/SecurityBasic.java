package com.dongah.fastcharger.websocket.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecurityBasic {
    private static final Logger logger = LoggerFactory.getLogger(SecurityBasic.class);

    // 최초 충전기 인증 로직
    public static String initRechgrAuthorization(String chargerId) {
        String processData = chargerId; // 충전소(6 자리) + 충전기 데이터(2 자리)
        String eqData = ""; // 암호화 로직 데이터
        int len = 4; // AES 최초 1 회 + 4 회 총 5 회 암호화 로직 실행
        try {
            eqData = initEncrypt(processData, processData); // AES 128bit 최초 1 회 암호화
            for (int i = 0; i < len; i++) {
                eqData = initEncrypt(eqData, processData); // AES 128bit 4 회 추가 암호화
            }
            MessageDigest md = MessageDigest.getInstance("SHA-1"); // SHA-1 단방향 암호화
            byte[] result = md.digest(eqData.getBytes());
            return bytesToHex(result); // 암호화 byte 데이터를 16 진수 문자열로 변환
        } catch (Exception e) {
            logger.error("initRechgrAuthorization error : {}", e.getMessage());
            return ""; // 예외 발생 시 빈 문자열 반환
        }
    }

    // AES 암호화 메서드
    private static String initEncrypt(String data, String key) throws Exception {
        // AES 키스펙 생성 (UTF-8 인코딩 사용)
        SecretKeySpec keySpec = generateAESKey(key, "UTF-8");
        Cipher cipher = Cipher.getInstance("AES"); // AES 암호화 객체 생성
        cipher.init(Cipher.ENCRYPT_MODE, keySpec); // 암호화 모드 설정
        return bytesToHex(cipher.doFinal(data.getBytes())); // 암호화 수행 후 16 진수 문자열 반환
    }

    // AES UTF-8 키 스펙 생성 128bit
    private static SecretKeySpec generateAESKey(final String key, final String encoding) {
        try {
            final byte[] finalKey = new byte[16]; // 128bit (16 바이트) 키 생성
            int i = 0;
            for (byte b : key.getBytes(encoding)) {
                finalKey[i++ % 16] ^= b; // 주어진 문자열 키를 바이트 배열로 변환 후 XOR 연산
            }
            return new SecretKeySpec(finalKey, "AES"); // SecretKeySpec 객체 생성
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // 인코딩 예외 발생 시 런타임 예외 발생
        }
    }

    // byte 배열을 16 진수 문자열로 변환
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
