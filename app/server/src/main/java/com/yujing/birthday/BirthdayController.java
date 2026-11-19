package com.yujing.birthday;

import com.yujing.birthday.dto.BirthdayRequest;
import com.yujing.birthday.dto.BirthdaySaveResult;
import com.yujing.birthday.dto.BirthdayView;
import com.yujing.common.api.ApiResponse;
import com.yujing.common.time.ClientZone;
import com.yujing.security.CurrentUserProvider;
import com.yujing.sync.SyncTimes;
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
@RequestMapping("/api/v1/birthdays")
public class BirthdayController {

    private final BirthdayService birthdayService;
    private final CurrentUserProvider currentUserProvider;

    public BirthdayController(BirthdayService birthdayService, CurrentUserProvider currentUserProvider) {
        this.birthdayService = birthdayService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String since,
                               HttpServletRequest request) {
        Long ownerId = currentUserProvider.requireId();
        ClientZone zone = ClientZone.from(request);
        if (since == null || since.isBlank()) {
            return ApiResponse.ok(birthdayService.list(ownerId, zone));
        }
        return ApiResponse.ok(birthdayService.changes(ownerId, SyncTimes.parseSince(since), zone));
    }

    @PostMapping
    public ApiResponse<BirthdaySaveResult> create(@RequestBody BirthdayRequest body,
                                                  HttpServletRequest request) {
        return ApiResponse.ok(birthdayService.create(currentUserProvider.requireId(), body,
                ClientZone.from(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<BirthdaySaveResult> update(@PathVariable Long id,
                                                  @RequestBody BirthdayRequest body,
                                                  HttpServletRequest request) {
        return ApiResponse.ok(birthdayService.update(currentUserProvider.requireId(), id, body,
                ClientZone.from(request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam long version) {
        birthdayService.delete(currentUserProvider.requireId(), id, version);
        return ApiResponse.ok();
    }
}
