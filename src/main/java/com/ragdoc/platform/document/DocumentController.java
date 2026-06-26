package com.ragdoc.platform.document;

import com.ragdoc.platform.document.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 인증된 사용자의 PDF 문서 CRUD API.
 * JWT에서 추출한 {@code userId}를 서비스에 전달해 소유권을 강제한다.
 */
@Tag(name = "Documents", description = "PDF 문서 업로드 및 관리 API")
@RestController
@RequestMapping("/api/documents")
@SecurityRequirement(name = "Bearer Authentication")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "문서 목록 조회")
    @GetMapping
    public List<DocumentResponse> listDocuments(@AuthenticationPrincipal Long userId) {
        return documentService.listDocuments(userId);
    }

    @Operation(summary = "문서 상세 조회")
    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId
    ) {
        return documentService.getDocument(userId, documentId);
    }

    @Operation(summary = "PDF 문서 업로드", description = "PDF를 업로드하고 텍스트 추출·청킹·임베딩을 수행합니다.")
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @AuthenticationPrincipal Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return documentService.uploadDocument(userId, file);
    }

    @Operation(summary = "문서 삭제")
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId
    ) {
        documentService.deleteDocument(userId, documentId);
    }
}
