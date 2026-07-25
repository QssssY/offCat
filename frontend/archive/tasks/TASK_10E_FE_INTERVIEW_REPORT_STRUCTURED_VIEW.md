### TASK_10E_FE_INTERVIEW_REPORT_STRUCTURED_VIEW

**类型：前端 Task**

**目标：​**  
将模拟面试评价报告页面从“原始 JSON 直接展示”改为“结构化展示 + 页面样式完善”，提升可读性和可用性。

**当前现状：​**
1. 面试评价报告页面已可打开
2. 已能获取：
   - 岗位信息
   - 综合评分
   - evaluationReport
3. 当前 `evaluationReport` 仍以原始 JSON 文本直接展示，缺少结构化布局和样式

**已知报告字段示例：​**
- `level`
- `summary`
- `strengths`
- `dimensions`
  - `systemDesign`
  - `communication`
  - `problemSolving`
  - `technicalDepth`
- `suggestions`
- `improvements`
- `overallScore`

**要求：​**
1. 将报告内容结构化展示，而不是直接输出原始 JSON
2. 页面至少拆分为以下可视区块：
   - 基本信息区（岗位、状态、综合分）
   - 总体评价/总结
   - 优势亮点
   - 各维度评分
   - 改进建议
   - 后续建议/提升方向
3. `dimensions` 用更易读的方式展示：
   - 卡片、分组、进度条、分数项均可
4. 数组类字段如 `strengths`、`suggestions`、`improvements` 使用列表展示
5. 做空值兼容：
   - 某字段不存在时不报错
   - 没有数据时显示合理空状态
6. 保持现有页面风格统一，不做无关重构

**输出要求：​**
1. 修改文件清单
2. 页面结构说明
3. 关键模板代码
4. 关键样式代码
5. 空状态处理说明