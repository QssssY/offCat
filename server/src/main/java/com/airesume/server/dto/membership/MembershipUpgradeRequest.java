package com.airesume.server.dto.membership;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MembershipUpgradeRequest {

    @NotBlank(message = "planCode can not be blank")
    private String planCode;
}
