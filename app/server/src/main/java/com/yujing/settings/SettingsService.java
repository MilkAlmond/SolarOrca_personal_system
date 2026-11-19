package com.yujing.settings;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.settings.dto.SettingsView;
import com.yujing.settings.mapper.UserSettingsMapper;
import com.yujing.settings.model.UserSettingsRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class SettingsService {

    private final UserSettingsMapper mapper;
    private final Clock clock;

    public SettingsService(UserSettingsMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public SettingsView get(Long ownerId) {
        UserSettingsRow row = mapper.findByOwner(ownerId);
        if (row == null) {
            mapper.insertDefault(ownerId, LocalDateTime.now(clock));
            row = mapper.findByOwner(ownerId);
        }
        if (row == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        return new SettingsView(row.isRemindersEnabled(), row.getVersion());
    }

    @Transactional
    public SettingsView update(Long ownerId, boolean remindersEnabled) {
        LocalDateTime now = LocalDateTime.now(clock);
        mapper.insertDefault(ownerId, now);
        mapper.updateEnabled(ownerId, remindersEnabled, now);
        UserSettingsRow row = mapper.findByOwner(ownerId);
        if (row == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        return new SettingsView(row.isRemindersEnabled(), row.getVersion());
    }
}
