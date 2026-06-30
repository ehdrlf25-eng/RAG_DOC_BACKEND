package com.ragdoc.platform.document.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/** PDF 원본 저장소 추상화 (로컬 디스크 또는 S3). */
public interface DocumentStorage {

    /**
     * PDF를 저장하고 이후 조회에 사용할 storage key를 반환한다.
     * <ul>
     *   <li>local: 파일 시스템 경로</li>
     *   <li>s3: 객체 키 (예: documents/{userId}/{uuid}.pdf)</li>
     * </ul>
     */
    String store(Long userId, MultipartFile file);

    InputStream open(String storageKey) throws IOException;

    boolean exists(String storageKey);

    void delete(String storageKey);
}
