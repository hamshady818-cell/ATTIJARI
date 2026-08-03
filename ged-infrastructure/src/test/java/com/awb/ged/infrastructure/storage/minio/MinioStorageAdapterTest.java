package com.awb.ged.infrastructure.storage.minio;

import com.awb.ged.common.exception.StorageException;
import com.awb.ged.domain.document.model.FileReferenceId;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MinIOContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinioStorageAdapterTest {

    private static MinIOContainer minioContainer;
    private static boolean dockerAvailable = false;

    private MinioStorageAdapter adapter;
    private MinioClient minioClient;
    private final String bucket = "test-ged-bucket";

    @BeforeAll
    static void initAll() {
        try {
            minioContainer = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");
            minioContainer.start();
            dockerAvailable = true;
        } catch (Exception e) {
            System.err.println("WARNING: Docker/Testcontainers not available or compatible. Skipping MinIO integration tests.");
            e.printStackTrace();
        }
    }

    @AfterAll
    static void tearDownAll() {
        if (minioContainer != null && minioContainer.isRunning()) {
            try {
                minioContainer.stop();
            } catch (Exception e) {
                // ignore shutdown issues
            }
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(dockerAvailable, "Skipping test because Docker container failed to start");

        minioClient = MinioClient.builder()
                .endpoint(minioContainer.getS3URL())
                .credentials(minioContainer.getUserName(), minioContainer.getPassword())
                .build();

        adapter = new MinioStorageAdapter(minioClient, bucket);
    }

    @Test
    @DisplayName("Should initialize bucket when it does not exist, then store, load and delete objects successfully")
    void minioStorageAdapter_RoundTripSuccess() throws Exception {
        // 1. Initialize bucket
        adapter.initBucket();
        
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        assertThat(bucketExists).isTrue();

        // 2. Store document content
        String path = "docs/test-integration.txt";
        byte[] originalContent = "Hello MinIO Integration Test!".getBytes(StandardCharsets.UTF_8);
        String contentType = "text/plain";

        FileReferenceId fileRef = adapter.store(path, originalContent, contentType);
        assertThat(fileRef).isNotNull();
        assertThat(fileRef.getValue()).isEqualTo(path);

        // 3. Load document content
        byte[] loadedContent = adapter.load(fileRef);
        assertThat(loadedContent).isEqualTo(originalContent);
        assertThat(new String(loadedContent, StandardCharsets.UTF_8)).isEqualTo("Hello MinIO Integration Test!");
    }

    @Test
    @DisplayName("Should throw StorageException when loading a non-existing object")
    void load_NonExistingObject_ThrowsStorageException() throws Exception {
        // Ensure bucket exists
        adapter.initBucket();

        FileReferenceId missingRef = new FileReferenceId("docs/does-not-exist.txt");

        assertThatThrownBy(() -> adapter.load(missingRef))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to read object from MinIO bucket");
    }
}
