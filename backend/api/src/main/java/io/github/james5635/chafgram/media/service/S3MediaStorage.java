package io.github.james5635.chafgram.media.service;

import io.github.james5635.chafgram.media.utils.MediaStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * S3-compatible media storage (Garage in dev, any S3 provider in production).
 * Enabled with {@code chafgram.media.backend=s3}.
 */
@Service
@ConditionalOnProperty(name = "chafgram.media.backend", havingValue = "s3")
public class S3MediaStorage implements MediaStorage {

    private final S3Client s3;
    private final String bucket;

    public S3MediaStorage(
            @Value("${chafgram.media.s3.endpoint:http://localhost:3900}") String endpoint,
            @Value("${chafgram.media.s3.bucket:chafgram-media}") String bucket,
            @Value("${chafgram.media.s3.region:garage}") String region,
            @Value("${chafgram.media.s3.access-key:}") String accessKey,
            @Value("${chafgram.media.s3.secret-key:}") String secretKey) {
        this.bucket = bucket;
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        // Garage does not understand the SDK's aws-chunked
                        // checksum payload used by newer AWS S3 endpoints.
                        .chunkedEncodingEnabled(false)
                        .checksumValidationEnabled(false)
                        .build())
                .build();
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        }
        catch (NoSuchBucketException e) {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    @Override
    public String store(MultipartFile file, String extension) {
        String key = "uploads/" + UUID.randomUUID() + extension;
        try {
            put(key, file.getBytes(), file.getContentType());
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to store media", e);
        }
        return key;
    }

    @Override
    public void storeProcessed(Path source, String storageKey) {
        try {
            put(storageKey, Files.readAllBytes(source), null);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to store processed media", e);
        }
        finally {
            try {
                Files.deleteIfExists(source);
            }
            catch (IOException ignored) {
                // A leftover temp file is harmless.
            }
        }
    }

    @Override
    public void delete(String storageKey) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }

    /**
     * S3 objects have no filesystem path. Download to a temp file so callers
     * that need a {@link Path} (image probing, ffmpeg) keep working.
     */
    @Override
    public Path resolvePath(String storageKey) {
        try {
            Path target = Files.createTempFile(
                    "chafgram-media-", "-" + storageKey.replace('/', '_'));
            try (InputStream in = read(storageKey).stream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to stage media " + storageKey, e);
        }
    }

    @Override
    public MediaStream read(String storageKey) {
        var object = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(storageKey).build());
        return new MediaStream(
                object,
                object.response().contentType(),
                object.response().contentLength());
    }

    private void put(String key, byte[] bytes, String contentType) {
        PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucket).key(key);
        if (contentType != null && !contentType.isBlank()) {
            request.contentType(contentType);
        }
        s3.putObject(request.build(), RequestBody.fromBytes(bytes));
    }
}
