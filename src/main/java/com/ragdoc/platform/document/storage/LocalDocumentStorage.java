package com.ragdoc.platform.document.storage;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.config.RagProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 로컬 디스크에 PDF를 저장한다 ({@code local} 프로필). */
@Component
@Profile({"local", "test"})
public class LocalDocumentStorage implements DocumentStorage {

    private final RagProperties ragProperties;

    public LocalDocumentStorage(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @Override
    public String store(Long userId, MultipartFile file) {
        try {
            Path directory = Path.of(ragProperties.storage().path(), String.valueOf(userId));
            Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + ".pdf");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    MessageKeys.DOCUMENT_STORAGE_FAILED,
                    ex
            );
        }
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(Path.of(storageKey));
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(Path.of(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(Path.of(storageKey));
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }
}
