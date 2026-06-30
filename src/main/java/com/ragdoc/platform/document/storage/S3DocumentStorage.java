package com.ragdoc.platform.document.storage;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.config.RagProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** S3에 PDF를 저장한다 ({@code cloud} 프로필, EC2 IAM Role 인증). */
@Component
@Profile("cloud")
public class S3DocumentStorage implements DocumentStorage {

    private final S3Client s3Client;
    private final RagProperties ragProperties;

    public S3DocumentStorage(S3Client s3Client, RagProperties ragProperties) {
        this.s3Client = s3Client;
        this.ragProperties = ragProperties;
    }

    @Override
    public String store(Long userId, MultipartFile file) {
        String objectKey = buildObjectKey(userId);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket())
                    .key(objectKey)
                    .contentType("application/pdf")
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return objectKey;
        } catch (IOException | S3Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    MessageKeys.DOCUMENT_STORAGE_FAILED,
                    ex
            );
        }
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket())
                    .key(storageKey)
                    .build());
        } catch (S3Exception ex) {
            throw new IOException("Failed to read S3 object: " + storageKey, ex);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket())
                    .key(storageKey)
                    .build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    MessageKeys.DOCUMENT_STORAGE_FAILED,
                    ex
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket())
                    .key(storageKey)
                    .build());
        } catch (S3Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    MessageKeys.DOCUMENT_STORAGE_FAILED,
                    ex
            );
        }
    }

    private String buildObjectKey(Long userId) {
        return "documents/" + userId + "/" + UUID.randomUUID() + ".pdf";
    }

    private String bucket() {
        String bucket = ragProperties.storage().bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.rag.storage.bucket is required for cloud profile");
        }
        return bucket;
    }
}
