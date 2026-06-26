package com.ragdoc.platform.document;

/** PDF 수집(텍스트 추출·청킹·임베딩) 진행 상태. */
public enum DocumentStatus {

    /** 수집 파이프라인 실행 중 */
    PROCESSING,

    /** 수집 완료, RAG 검색 대상 */
    READY,

    /** 수집 실패. 청크·임베딩이 없거나 불완전할 수 있음 */
    FAILED
}
