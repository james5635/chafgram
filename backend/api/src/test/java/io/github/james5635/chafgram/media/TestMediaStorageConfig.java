package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.media.utils.MediaStorage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces filesystem/S3 storage in integration tests. */
@TestConfiguration
public class TestMediaStorageConfig {

    @Bean
    @Primary
    MediaStorage inMemoryMediaStorage() {
        return new InMemoryMediaStorage();
    }
}
