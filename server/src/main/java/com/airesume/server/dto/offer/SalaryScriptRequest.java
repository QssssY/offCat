package com.airesume.server.dto.offer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 谈薪话术模板请求。
 */
@Data
public class SalaryScriptRequest {

    /**
     * 目标公司名称，用于生成更贴近场景的话术。
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
     * 候选人核心背景。
     */
    @NotBlank(message = "候选人背景不能为空")
    @Size(max = 1200, message = "候选人背景不能超过1200个字符")
    private String candidateBackground;

    /**
     * 期望薪资。
     */
    @NotBlank(message = "期望薪资不能为空")
    @Size(max = 80, message = "期望薪资不能超过80个字符")
    private String expectedSalary;

    /**
     * 当前 Offer 或对方报价。
     */
    @Size(max = 80, message = "当前报价不能超过80个字符")
    private String offerSalary;

    /**
     * 谈判目标，例如争取年包、签字费、试用期薪资等。
     */
    @NotBlank(message = "谈判目标不能为空")
    @Size(max = 300, message = "谈判目标不能超过300个字符")
    private String negotiationGoal;
}
