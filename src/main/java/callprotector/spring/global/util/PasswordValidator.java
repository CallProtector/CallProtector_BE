package callprotector.spring.global.util;

import java.util.regex.Pattern;

// 비밀번호 조건 설정
public class PasswordValidator {
    private static final Pattern VALID_PASSWORD =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+{};:,<.>]).{8,16}$");

    public static boolean isValid(String password) {
        return password != null && VALID_PASSWORD.matcher(password).matches();
    }
}
