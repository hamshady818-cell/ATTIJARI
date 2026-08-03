package com.awb.ged.application.port.out.event;

import com.awb.ged.common.event.DomainEvent;

public interface EventPublisherPort {

    void publish(DomainEvent event);
}
