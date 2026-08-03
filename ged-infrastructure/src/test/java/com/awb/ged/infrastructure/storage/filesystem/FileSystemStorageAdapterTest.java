package com.awb.ged.infrastructure.storage.filesystem;

import com.awb.ged.common.exception.StorageException;
import com.awb.ged.domain.document.model.FileReferenceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageAdapterTest {

    @TempDir
    Path tempDir;

    private FileSystemStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileSystemStorageAdapter(tempDir.toString());
    }

    @Test
    @DisplayName("Should successfully store and load binary content on filesystem")
    void storeAndLoad_Success() {
        // Given
        String relativePath = "documents/doc-1/v1.pdf";
        byte[] content = "Hello GED Attijari Storage".getBytes();

        // When
        FileReferenceId fileRef = adapter.store(relativePath, content, "application/pdf");
        byte[] loadedContent = adapter.load(fileRef);

        // Then
        assertThat(fileRef).isNotNull();
        assertThat(fileRef.getValue()).isEqualTo(relativePath);
        assertThat(loadedContent).isEqualTo(content);
    }

    @Test
    @DisplayName("Should throw StorageException when file reference does not exist on disk")
    void load_FileNotFound_ThrowsStorageException() {
        // Given
        FileReferenceId invalidRef = new FileReferenceId("non-existent/file.pdf");

        // When / Then
        assertThatThrownBy(() -> adapter.load(invalidRef))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("File not found on filesystem");
    }

    @Test
    @DisplayName("Should successfully delete file from filesystem")
    void delete_Success() {
        // Given
        String relativePath = "documents/doc-2/v1.pdf";
        byte[] content = "To be deleted".getBytes();
        FileReferenceId fileRef = adapter.store(relativePath, content, "application/pdf");

        // When
        adapter.delete(fileRef);

        // Then
        assertThatThrownBy(() -> adapter.load(fileRef))
                .isInstanceOf(StorageException.class);
    }
}
