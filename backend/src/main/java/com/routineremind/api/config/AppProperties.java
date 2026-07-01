package com.routineremind.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routineremind")
public class AppProperties {

    private final Gcp gcp = new Gcp();
    private final Cors cors = new Cors();
    private final Reminders reminders = new Reminders();

    public Gcp getGcp() {
        return gcp;
    }

    public Cors getCors() {
        return cors;
    }

    public Reminders getReminders() {
        return reminders;
    }

    public static class Gcp {
        private String projectId;
        private String storageBucket;
        private String serviceAccountPath;

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getStorageBucket() {
            return storageBucket;
        }

        public void setStorageBucket(String storageBucket) {
            this.storageBucket = storageBucket;
        }

        public String getServiceAccountPath() {
            return serviceAccountPath;
        }

        public void setServiceAccountPath(String serviceAccountPath) {
            this.serviceAccountPath = serviceAccountPath;
        }
    }

    public static class Cors {
        private String allowedOrigins = "";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String[] originsArray() {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                return new String[0];
            }
            return allowedOrigins.split("\\s*,\\s*");
        }
    }

    public static class Reminders {
        private String schedulerToken = "";

        public String getSchedulerToken() {
            return schedulerToken;
        }

        public void setSchedulerToken(String schedulerToken) {
            this.schedulerToken = schedulerToken;
        }

        public boolean hasSchedulerToken() {
            return schedulerToken != null && !schedulerToken.isBlank();
        }
    }
}
