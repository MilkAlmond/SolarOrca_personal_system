package com.yujing.task;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.time.ClientZone;
import com.yujing.security.CurrentUserProvider;
import com.yujing.task.dto.TaskCheckinRequest;
import com.yujing.task.dto.TaskCheckinResult;
import com.yujing.task.dto.TaskRequest;
import com.yujing.task.dto.TaskView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserProvider currentUserProvider;

    public TaskController(TaskService taskService, CurrentUserProvider currentUserProvider) {
        this.taskService = taskService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String since,
                               HttpServletRequest request) {
        Long ownerId = currentUserProvider.requireId();
        ClientZone zone = ClientZone.from(request);
        if (since == null || since.isBlank()) {
            return ApiResponse.ok(taskService.list(ownerId, zone));
        }
        return ApiResponse.ok(taskService.changes(ownerId,
                com.yujing.sync.SyncTimes.parseSince(since), zone));
    }

    @GetMapping("/today")
    public ApiResponse<List<TaskView>> today(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        return ApiResponse.ok(taskService.today(currentUserProvider.requireId(), date,
                ClientZone.from(request)));
    }

    @PostMapping
    public ApiResponse<TaskView> create(@RequestBody TaskRequest body,
                                        HttpServletRequest request) {
        return ApiResponse.ok(taskService.create(currentUserProvider.requireId(), body,
                ClientZone.from(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskView> update(@PathVariable Long id,
                                        @RequestBody TaskRequest body,
                                        HttpServletRequest request) {
        return ApiResponse.ok(taskService.update(currentUserProvider.requireId(), id, body,
                ClientZone.from(request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam long version) {
        taskService.delete(currentUserProvider.requireId(), id, version);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/checkins")
    public ApiResponse<TaskCheckinResult> checkin(@PathVariable Long id,
                                                  @RequestBody TaskCheckinRequest body) {
        return ApiResponse.ok(taskService.checkin(currentUserProvider.requireId(), id,
                body == null ? null : body.date()));
    }

    @DeleteMapping("/{id}/checkins")
    public ApiResponse<TaskView> undoCheckin(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(taskService.undoCheckin(currentUserProvider.requireId(), id, date));
    }
}
