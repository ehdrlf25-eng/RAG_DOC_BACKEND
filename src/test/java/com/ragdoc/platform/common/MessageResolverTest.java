package com.ragdoc.platform.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MessageResolverTest {

    @Autowired
    private MessageResolver messageResolver;

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void resolvesKoreanMessageByDefault() {
        LocaleContextHolder.setLocale(Locale.KOREAN);

        String message = messageResolver.resolve(MessageKeys.AUTH_INVALID_CREDENTIALS);

        assertThat(message).isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void resolvesEnglishMessage() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        String message = messageResolver.resolve(MessageKeys.AUTH_INVALID_CREDENTIALS);

        assertThat(message).isEqualTo("Invalid email or password.");
    }
}
