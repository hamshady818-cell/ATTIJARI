package com.awb.ged.infrastructure.event;

import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.common.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}
