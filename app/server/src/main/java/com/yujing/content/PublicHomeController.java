package com.yujing.content;

import com.yujing.common.api.ApiResponse;
import com.yujing.content.dto.PublicHomeView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicHomeController {

    private final PublicContentService publicContentService;

    public PublicHomeController(PublicContentService publicContentService) {
        this.publicContentService = publicContentService;
    }

    @GetMapping("/home")
    public ApiResponse<PublicHomeView> home() {
        return ApiResponse.ok(publicContentService.home());
    }
}
