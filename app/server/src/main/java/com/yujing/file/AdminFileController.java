package com.yujing.file;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.web.ClientMeta;
import com.yujing.file.dto.FileView;
import com.yujing.file.model.FileRow;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/admin/files")
public class AdminFileController {

    private final FileService fileService;
    private final CurrentUserProvider currentUserProvider;

    public AdminFileController(FileService fileService, CurrentUserProvider currentUserProvider) {
        this.fileService = fileService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ApiResponse<FileView> upload(@RequestParam("purpose") String purpose,
                                        @RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {
        return ApiResponse.ok(fileService.upload(file, purpose, currentUserProvider.require(),
                ClientMeta.from(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> read(@PathVariable Long id) {
        FileRow row = fileService.requireFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(fileService.read(row));
    }
}
