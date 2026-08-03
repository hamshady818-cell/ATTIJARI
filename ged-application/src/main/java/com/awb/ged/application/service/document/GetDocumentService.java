package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.GetDocumentUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetDocumentService implements GetDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final DocumentMapper documentMapper;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public GetDocumentService(DocumentRepositoryPort documentRepositoryPort,
                              DocumentMapper documentMapper,
                              EventPublisherPort eventPublisherPort) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.documentMapper = documentMapper;
        this.eventPublisherPort = eventPublisherPort;
    }

    public GetDocumentService(DocumentRepositoryPort documentRepositoryPort,
                              DocumentMapper documentMapper) {
        this(documentRepositoryPort, documentMapper, null);
    }

    @Override
    public DocumentResponseDto getDocumentById(UUID documentId) {
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        if (eventPublisherPort != null) {
            eventPublisherPort.publish(new com.awb.ged.domain.document.event.DocumentViewedEvent(
                    document.getId(),
                    document.getName(),
                    document.getOwnerId()
            ));
        }

        return documentMapper.toResponseDto(document);
    }
}
