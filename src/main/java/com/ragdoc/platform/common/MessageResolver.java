package com.ragdoc.platform.common;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring MessageSource 래퍼.
 * Accept-Language 헤더로 결정된 로케일에 맞는 에러·검증 메시지를 반환한다.
 */
@Component
public class MessageResolver {

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 메시지 키를 현재 요청 로케일로 변환한다.
     * 키가 없으면 코드 문자열 자체를 반환한다(fallback).
     */
    public String resolve(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }
}
