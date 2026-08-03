package com.awb.ged.infrastructure.storage.minio;

import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.StorageException;
import com.awb.ged.domain.document.model.FileReferenceId;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@ConditionalOnProperty(name = "ged.storage.type", havingValue = "minio")
public class MinioStorageAdapter implements StoragePort {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageAdapter(MinioClient minioClient,
                               @Value("${ged.storage.minio.bucket:ged-documents}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new StorageException(
                    ErrorCode.STORAGE_WRITE_ERROR,
                    "Failed to initialize MinIO bucket: " + bucket,
                    e
            );
        }
    }

    @Override
    public FileReferenceId store(String path, byte[] content, String mimeType) {
        try {
            byte[] data = content != null ? content : new byte[0];
            String contentType = mimeType != null ? mimeType : "application/octet-stream";

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(path)
                                .stream(bais, data.length, -1)
                                .contentType(contentType)
                                .build()
                );
            }

            return new FileReferenceId(path);
        } catch (Exception e) {
            throw new StorageException(
                    ErrorCode.STORAGE_WRITE_ERROR,
                    "Failed to upload object to MinIO bucket " + bucket + " at path: " + path,
                    e
            );
        }
    }

    @Override
    public byte[] load(FileReferenceId fileReferenceId) {
        if (fileReferenceId == null || fileReferenceId.getValue() == null) {
            throw new StorageException(ErrorCode.STORAGE_READ_ERROR, "FileReferenceId cannot be null");
        }

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileReferenceId.getValue())
                        .build()
        )) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new StorageException(
                    ErrorCode.STORAGE_READ_ERROR,
                    "Failed to read object from MinIO bucket " + bucket + " at path: " + fileReferenceId.getValue(),
                    e
            );
        }
    }

    @Override
    public void delete(FileReferenceId fileReferenceId) {
        if (fileReferenceId == null || fileReferenceId.getValue() == null) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileReferenceId.getValue())
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(
                    ErrorCode.STORAGE_DELETE_ERROR,
                    "Failed to delete object from MinIO bucket " + bucket + " at path: " + fileReferenceId.getValue(),
                    e
            );
        }
    }
}
