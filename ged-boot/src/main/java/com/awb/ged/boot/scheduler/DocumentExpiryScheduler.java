package com.awb.ged.boot.scheduler;

import com.awb.ged.application.port.in.document.ExpireDocumentsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <h1>DocumentExpiryScheduler</h1>
 * <p>
 * Infrastructure scheduler that triggers the document-expiry use case every day at 02:00.
 * This class is intentionally free of business logic — it only delegates to
 * {@link ExpireDocumentsUseCase} and logs the result.
 * </p>
 *
 * <p>Scheduling is enabled globally via {@code @EnableScheduling} on
 * {@code GedAwbApplication}.</p>
 *
 * <p>Cron expression {@code "0 0 2 * * *"} = every day at 02:00:00 server time.</p>
 */
@Component
public class DocumentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentExpiryScheduler.class);

    private final ExpireDocumentsUseCase expireDocumentsUseCase;

    @Autowired
    public DocumentExpiryScheduler(ExpireDocumentsUseCase expireDocumentsUseCase) {
        this.expireDocumentsUseCase = expireDocumentsUseCase;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void expireOverdueDocuments() {
        int count = expireDocumentsUseCase.expireOverdueDocuments();
        log.info("[DocumentExpiryScheduler] Tâche planifiée exécutée : {} document(s) archivé(s)", count);
    }
}
