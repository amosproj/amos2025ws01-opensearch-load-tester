package com.opensearchloadtester.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FinishLoadTestDto {

    @NotBlank
    private final String loadGeneratorId;
    private final boolean success;
    private final String errorMessage;
}
