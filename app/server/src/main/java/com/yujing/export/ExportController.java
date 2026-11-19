package com.yujing.export;

import com.yujing.common.api.ApiResponse;
import com.yujing.export.dto.ExportJobView;
import com.yujing.security.CurrentUserProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private final ExportService exportService;
    private final CurrentUserProvider currentUserProvider;

    public ExportController(ExportService exportService, CurrentUserProvider currentUserProvider) {
        this.exportService = exportService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ApiResponse<ExportJobView> create() {
        return ApiResponse.ok(exportService.create(currentUserProvider.requireId()));
    }

    @GetMapping("/latest")
    public ApiResponse<ExportJobView> latest() {
        return ApiResponse.ok(exportService.latest(currentUserProvider.requireId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExportJobView> get(@PathVariable Long id) {
        return ApiResponse.ok(exportService.get(currentUserProvider.requireId(), id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ExportService.ExportDownload download =
                exportService.download(currentUserProvider.requireId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(download.content());
    }
}
