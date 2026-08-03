package com.awb.ged.infrastructure.storage.filesystem;

import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.StorageException;
import com.awb.ged.domain.document.model.FileReferenceId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConditionalOnProperty(name = "ged.storage.type", havingValue = "filesystem", matchIfMissing = true)
public class FileSystemStorageAdapter implements StoragePort {

    private final Path rootPath;

    public FileSystemStorageAdapter(@Value("${ged.storage.base-path:./storage-data}") String basePath) {
        this.rootPath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public FileReferenceId store(String path, byte[] content, String mimeType) {
        try {
            Path targetPath = rootPath.resolve(path).normalize();
            if (!targetPath.startsWith(rootPath)) {
                throw new StorageException(ErrorCode.STORAGE_WRITE_ERROR, "Invalid storage path outside root directory");
            }

            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content != null ? content : new byte[0]);

            return new FileReferenceId(path);
        } catch (IOException e) {
            throw new StorageException(ErrorCode.STORAGE_WRITE_ERROR, "Failed to write file to filesystem: " + path, e);
        }
    }

    @Override
    public byte[] load(FileReferenceId fileReferenceId) {
        if (fileReferenceId == null || fileReferenceId.getValue() == null) {
            throw new StorageException(ErrorCode.STORAGE_READ_ERROR, "FileReferenceId cannot be null");
        }

        try {
            Path targetPath = rootPath.resolve(fileReferenceId.getValue()).normalize();
            if (!targetPath.startsWith(rootPath) || !Files.exists(targetPath)) {
                throw new StorageException(
                        ErrorCode.STORAGE_READ_ERROR,
                        "File not found on filesystem: " + fileReferenceId.getValue()
                );
            }

            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new StorageException(
                    ErrorCode.STORAGE_READ_ERROR,
                    "Failed to read file from filesystem: " + fileReferenceId.getValue(),
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
            Path targetPath = rootPath.resolve(fileReferenceId.getValue()).normalize();
            if (targetPath.startsWith(rootPath) && Files.exists(targetPath)) {
                Files.delete(targetPath);
            }
        } catch (IOException e) {
            throw new StorageException(
                    ErrorCode.STORAGE_DELETE_ERROR,
                    "Failed to delete file from filesystem: " + fileReferenceId.getValue(),
                    e
            );
        }
    }
}
