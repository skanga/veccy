package com.veccy.health;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a health check.
 */
public class HealthCheckResult {

    private final HealthStatus status;
    private final String message;
    private final Map<String, Object> details;
    private final Instant timestamp;
    private final Throwable error;

    private HealthCheckResult(Builder builder) {
        this.status = Objects.requireNonNull(builder.status, "status cannot be null");
        this.message = builder.message;
        this.details = new HashMap<>(builder.details);
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.error = builder.error;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isHealthy() {
        return status == HealthStatus.UP;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static HealthCheckResult up() {
        return builder().status(HealthStatus.UP).build();
    }

    public static HealthCheckResult up(String message) {
        return builder().status(HealthStatus.UP).message(message).build();
    }

    public static HealthCheckResult down() {
        return builder().status(HealthStatus.DOWN).build();
    }

    public static HealthCheckResult down(String message) {
        return builder().status(HealthStatus.DOWN).message(message).build();
    }

    public static HealthCheckResult down(Throwable error) {
        return builder().status(HealthStatus.DOWN).error(error).build();
    }

    public static HealthCheckResult degraded(String message) {
        return builder().status(HealthStatus.DEGRADED).message(message).build();
    }

    public static class Builder {
        private HealthStatus status;
        private String message;
        private Map<String, Object> details = new HashMap<>();
        private Instant timestamp;
        private Throwable error;

        public Builder status(HealthStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details.putAll(details);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            if (error != null && message == null) {
                this.message = error.getMessage();
            }
            return this;
        }

        public HealthCheckResult build() {
            return new HealthCheckResult(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HealthCheckResult{");
        sb.append("status=").append(status);
        if (message != null) {
            sb.append(", message='").append(message).append('\'');
        }
        if (!details.isEmpty()) {
            sb.append(", details=").append(details);
        }
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }
}
