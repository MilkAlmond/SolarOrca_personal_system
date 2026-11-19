package com.yujing.health;

import com.yujing.common.api.ApiResponse;
import com.yujing.common.error.ErrorCode;
import com.yujing.config.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;
    private final AppProperties properties;
    private final Clock clock;

    public HealthController(DataSource dataSource, AppProperties properties, Clock clock) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthView>> health() {
        boolean databaseUp = databaseUp();
        String time = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).toString();
        if (!databaseUp) {
            HealthView view = new HealthView("DOWN", "DOWN", time, properties.appVersion());
            return ResponseEntity.status(ErrorCode.SERVICE_UNAVAILABLE.httpStatus())
                    .body(ApiResponse.error(ErrorCode.SERVICE_UNAVAILABLE.code(),
                            ErrorCode.SERVICE_UNAVAILABLE.message(), view));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                new HealthView("UP", "UP", time, properties.appVersion())));
    }

    private boolean databaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }

    public record HealthView(String status, String database, String time, String version) {
    }
}
