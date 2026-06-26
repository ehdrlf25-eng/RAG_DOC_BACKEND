package com.ragdoc.platform.common;

/**
 * i18n 메시지 키 상수.
 * {@link com.ragdoc.platform.config.GlobalExceptionHandler}에서 키를 받아
 * {@link MessageResolver}로 로컬라이즈된 문구를 반환한다.
 */
public final class MessageKeys {

    // --- 인증 ---
    public static final String AUTH_INVALID_CREDENTIALS = "error.auth.invalid_credentials";
    public static final String AUTH_EMAIL_ALREADY_EXISTS = "error.auth.email_already_exists";

    // --- 사용자 ---
    public static final String USER_NOT_FOUND = "error.user.not_found";

    // --- 공통 ---
    public static final String COMMON_INVALID_INPUT = "error.common.invalid_input";
    public static final String COMMON_REQUEST_FAILED = "error.common.request_failed";
    public static final String COMMON_UNEXPECTED = "error.common.unexpected";

    // --- 문서 (소유권·업로드) ---
    public static final String DOCUMENT_NOT_FOUND = "error.document.not_found";
    public static final String DOCUMENT_ACCESS_DENIED = "error.document.access_denied";
    public static final String DOCUMENT_FILE_REQUIRED = "error.document.file_required";
    public static final String DOCUMENT_INVALID_TYPE = "error.document.invalid_type";
    public static final String DOCUMENT_PROCESSING_FAILED = "error.document.processing_failed";
    public static final String DOCUMENT_STORAGE_FAILED = "error.document.storage_failed";

    // --- 대화 (소유권) ---
    public static final String CONVERSATION_NOT_FOUND = "error.conversation.not_found";
    public static final String CONVERSATION_ACCESS_DENIED = "error.conversation.access_denied";

    private MessageKeys() {
    }
}
