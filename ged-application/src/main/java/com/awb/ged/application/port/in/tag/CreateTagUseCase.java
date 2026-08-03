package com.awb.ged.application.port.in.tag;

import com.awb.ged.application.dto.tag.CreateTagCommand;
import com.awb.ged.application.dto.tag.TagResponseDto;

public interface CreateTagUseCase {
    TagResponseDto createTag(CreateTagCommand command);
}
