package com.marinbom2.finbroker.util;

import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationHelperUtils {

    /** 비밀번호 6~20자. 영문 대소문자·숫자·특수문자 중 두 가지 이상 조합 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?:(?=.*[a-z])(?=.*[A-Z])|(?=.*[a-z])(?=.*\\d)|(?=.*[A-Z])(?=.*\\d)|(?=.*[@#$%^&+=!~*()_\\-{}\\[\\]:;\"'<>,.?\\/\\\\]))"
                    + "[a-zA-Z\\d@#$%^&+=!~*()_\\-{}\\[\\]:;\"'<>,.?\\/\\\\]{6,20}$");

    public static boolean isInValidPassword(String password) {
        return password == null || !PASSWORD_PATTERN.matcher(password).matches();
    }
}
