package com.awb.ged.application.port.out.storage;

import com.awb.ged.domain.document.model.FileReferenceId;

public interface StoragePort {

    FileReferenceId store(String path, byte[] content, String mimeType);

    byte[] load(FileReferenceId fileReferenceId);

    void delete(FileReferenceId fileReferenceId);
}
