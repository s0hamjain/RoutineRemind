package com.routineremind.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final AppProperties props;

    public FirebaseConfig(AppProperties props) {
        this.props = props;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials = loadCredentials();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(props.getGcp().getProjectId())
                .setStorageBucket(props.getGcp().getStorageBucket())
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("Initialized FirebaseApp for project '{}'", props.getGcp().getProjectId());
        return app;
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }

    @Bean
    public Storage storage() throws IOException {
        return StorageOptions.newBuilder()
                .setProjectId(props.getGcp().getProjectId())
                .setCredentials(loadCredentials())
                .build()
                .getService();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        String path = props.getGcp().getServiceAccountPath();
        if (path != null && !path.isBlank() && Files.exists(Path.of(path))) {
            try (InputStream in = new FileInputStream(path)) {
                log.info("Loading service-account credentials from {}", path);
                return GoogleCredentials.fromStream(in);
            }
        }
        // Fall back to GOOGLE_APPLICATION_CREDENTIALS / ADC.
        log.info("Service-account file not found at '{}', falling back to Application Default Credentials", path);
        return GoogleCredentials.getApplicationDefault();
    }
}
