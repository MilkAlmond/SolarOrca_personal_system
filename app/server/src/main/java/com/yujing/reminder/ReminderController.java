package com.yujing.reminder;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.time.ClientZone;
import com.yujing.common.web.ClientMeta;
import com.yujing.reminder.dto.CreateReminderRequest;
import com.yujing.reminder.dto.ReminderReceiptRequest;
import com.yujing.reminder.dto.ReminderView;
import com.yujing.reminder.dto.UpdateReminderRequest;
import com.yujing.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final CurrentUserProvider currentUserProvider;

    public ReminderController(ReminderService reminderService, CurrentUserProvider currentUserProvider) {
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String since) {
        Long ownerId = currentUserProvider.requireId();
        if (since == null || since.isBlank()) {
            return ApiResponse.ok(reminderService.list(ownerId));
        }
        return ApiResponse.ok(reminderService.changes(ownerId,
                com.yujing.sync.SyncTimes.parseSince(since)));
    }

    @PostMapping
    public ApiResponse<ReminderView> create(@Valid @RequestBody CreateReminderRequest body,
                                            HttpServletRequest request) {
        return ApiResponse.ok(reminderService.createManual(currentUserProvider.requireId(), body,
                ClientZone.from(request)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ReminderView> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateReminderRequest body) {
        return ApiResponse.ok(reminderService.setEnabled(currentUserProvider.requireId(), id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam long version) {
        reminderService.delete(currentUserProvider.requireId(), id, version);
        return ApiResponse.ok();
    }

    @GetMapping("/due")
    public ApiResponse<List<ReminderView>> due(HttpServletRequest request) {
        String deviceId = ClientMeta.from(request).deviceId();
        return ApiResponse.ok(reminderService.due(currentUserProvider.requireId(), deviceId));
    }

    @PostMapping("/{id}/receipts")
    public ApiResponse<ReminderView> receipt(@PathVariable Long id,
                                             @Valid @RequestBody ReminderReceiptRequest body,
                                             HttpServletRequest request) {
        String headerDeviceId = ClientMeta.from(request).deviceId();
        return ApiResponse.ok(reminderService.receipt(currentUserProvider.requireId(), id, body,
                headerDeviceId));
    }
}
