package com.awb.ged.application.port.out.storage;

import com.awb.ged.domain.document.model.FileReferenceId;

import java.io.InputStream;

public interface StoragePort {

    FileReferenceId store(String path, byte[] content, String mimeType);

    byte[] load(FileReferenceId fileReferenceId);

    /**
     * Returns an InputStream for streaming large files without loading all bytes into memory.
     */
    InputStream loadStream(FileReferenceId fileReferenceId);

    void delete(FileReferenceId fileReferenceId);
}
