package com.yujing.course;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.time.ClientZone;
import com.yujing.course.dto.CourseRequest;
import com.yujing.course.dto.CourseSaveResult;
import com.yujing.course.dto.CourseView;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final CurrentUserProvider currentUserProvider;

    public CourseController(CourseService courseService, CurrentUserProvider currentUserProvider) {
        this.courseService = courseService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String since) {
        Long ownerId = currentUserProvider.requireId();
        if (since == null || since.isBlank()) {
            return ApiResponse.ok(courseService.list(ownerId));
        }
        return ApiResponse.ok(courseService.changes(ownerId,
                com.yujing.sync.SyncTimes.parseSince(since)));
    }

    @PostMapping
    public ApiResponse<CourseSaveResult> create(@RequestBody CourseRequest body,
                                                HttpServletRequest request) {
        return ApiResponse.ok(courseService.create(currentUserProvider.requireId(), body,
                ClientZone.from(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseSaveResult> update(@PathVariable Long id,
                                                @RequestBody CourseRequest body,
                                                HttpServletRequest request) {
        return ApiResponse.ok(courseService.update(currentUserProvider.requireId(), id, body,
                ClientZone.from(request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam long version) {
        courseService.delete(currentUserProvider.requireId(), id, version);
        return ApiResponse.ok();
    }
}
