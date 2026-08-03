package com.awb.ged.infrastructure.storage.minio.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ged.storage.type", havingValue = "minio")
public class MinioStorageConfiguration {

    @Bean
    public MinioClient minioClient(
            @Value("${ged.storage.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${ged.storage.minio.access-key:minioadmin}") String accessKey,
            @Value("${ged.storage.minio.secret-key:minioadmin}") String secretKey) {

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
