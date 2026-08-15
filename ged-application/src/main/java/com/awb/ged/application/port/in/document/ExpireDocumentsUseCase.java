package com.awb.ged.application.port.in.document;

/**
 * <h1>ExpireDocumentsUseCase</h1>
 * <p>
 * Input port responsible for archiving all active documents whose
 * {@code expirationDate} has passed.
 * </p>
 * <p>
 * The triggering mechanism (e.g., {@code @Scheduled}, HTTP endpoint, or manual call)
 * is intentionally kept outside this interface and lives in the {@code ged-boot} module.
 * </p>
 */
public interface ExpireDocumentsUseCase {

    /**
     * Archives every {@code PUBLISHED} or {@code DRAFT} document whose
     * {@code expirationDate} is strictly before today.
     *
     * @return the number of documents that were archived during this run
     */
    int expireOverdueDocuments();
}
