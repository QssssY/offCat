package com.airesume.server.dto.offer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 薪资谈判模拟请求。
 */
@Data
public class SalaryNegotiationSimulationRequest {

    /**
     * 目标公司名称，用于限定谈薪场景，不接实时行情。
     */
    @Size(max = 80, message = "公司名称不能超过80个字符")
    private String companyName;

    /**
     * 目标岗位。
     */
    @NotBlank(message = "目标岗位不能为空")
    @Size(max = 80, message = "目标岗位不能超过80个字符")
    private String jobTitle;

    /**
     * 工作年限或经验描述。
     */
    @Size(max = 40, message = "经验描述不能超过40个字符")
    private String experienceYears;

    /**
     * 当前薪资，可填写月薪、年包或区间。
     */
    @Size(max = 80, message = "当前薪资不能超过80个字符")
    private String currentSalary;

    /**
     * 期望薪资，可填写月薪、年包或区间。
     */
    @NotBlank(message = "期望薪资不能为空")
    @Size(max = 80, message = "期望薪资不能超过80个字符")
    private String expectedSalary;

    /**
     * 对方当前报价。
     */
    @Size(max = 80, message = "当前报价不能超过80个字符")
    private String offerSalary;

    /**
     * 候选人背景摘要。
     */
    @NotBlank(message = "候选人背景不能为空")
    @Size(max = 1200, message = "候选人背景不能超过1200个字符")
    private String candidateBackground;

    /**
     * HR 当前抛出的问题或场景。
     */
    @NotBlank(message = "HR问题不能为空")
    @Size(max = 500, message = "HR问题不能超过500个字符")
    private String hrMessage;
}
