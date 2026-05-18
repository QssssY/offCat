package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VersionLogCreateRequest {

    @NotBlank(message = "版本号不能为空")
    private String version;

    @NotBlank(message = "版本标题不能为空")
    private String title;

    @NotBlank(message = "更新内容不能为空")
    private String content;

    @NotBlank(message = "版本类型不能为空")
    @Pattern(regexp = "major|minor|patch", message = "版本类型仅支持 major/minor/patch")
    private String type;

    private Integer status;
}
