package com.storage.design.dto;

import java.util.List;

public record DesignGuideFilterOptionsVO(
        List<DesignProductTypeOptionVO> productTypes,
        List<DesignStageOptionVO> stages,
        List<String> scopes
) {
}
