package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.notification.NotificationResponseDto;
import com.awb.ged.domain.notification.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponseDto toResponseDto(Notification notification);
}
