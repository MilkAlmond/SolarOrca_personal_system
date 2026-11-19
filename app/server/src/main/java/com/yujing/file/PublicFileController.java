package com.yujing.file;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.content.PublicContentService;
import com.yujing.file.model.FileRow;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Images uploaded for drafts stay private: a public request only succeeds when
 * the file is referenced by the published singleton content (plan 5.3).
 */
@RestController
@RequestMapping("/api/v1/public/files")
public class PublicFileController {

    private final FileService fileService;
    private final PublicContentService publicContentService;

    public PublicFileController(FileService fileService, PublicContentService publicContentService) {
        this.fileService = fileService;
        this.publicContentService = publicContentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> read(@PathVariable Long id) {
        if (!publicContentService.isPublishedFile(id)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        FileRow row = fileService.requireFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(fileService.read(row));
    }
}
