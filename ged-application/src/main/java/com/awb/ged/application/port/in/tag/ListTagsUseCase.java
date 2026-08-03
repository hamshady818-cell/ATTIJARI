package com.awb.ged.application.port.in.tag;

import com.awb.ged.application.dto.tag.TagResponseDto;

import java.util.List;

public interface ListTagsUseCase {
    List<TagResponseDto> listTags();
}
