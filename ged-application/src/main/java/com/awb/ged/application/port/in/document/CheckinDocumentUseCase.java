package com.awb.ged.application.port.in.document;

import java.util.UUID;

public interface CheckinDocumentUseCase {

    void checkin(UUID documentId, UUID userId);
}
