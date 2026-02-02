package com.opensearchloadtester.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinishLoadTestDto {

    @NotBlank
    private String loadGeneratorId;
    private boolean success;
    private String errorMessage;
}
