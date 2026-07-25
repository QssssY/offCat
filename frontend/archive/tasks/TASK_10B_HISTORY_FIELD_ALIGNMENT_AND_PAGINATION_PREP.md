- ### TASK_10B_HISTORY_FIELD_ALIGNMENT_AND_PAGINATION_ADAPT

  **目标：​**  
  修复前端历史页面，使其正确适配当前后端“分页返回”的面试历史接口，并与当前后端真实字段保持一致，解决历史列表字段显示不一致、面试模式/消息数来源不稳定，以及前端 `sort is not a function` 报错问题。

  **当前背景：​**  
  项目已经完成一轮后端修复，当前面试历史接口不再直接返回数组，而是分页对象。前端仍然把 `res.data` 当数组处理并直接调用 `.sort()`，因此出现报错。  
  另外，历史字段展示必须以当前后端真实返回和数据库真实结构为准，不能再继续假设不存在的字段。

  **已知现状：​**
  1. 前端报错：
     - `HomeView.vue: TypeError: (res.data || []).sort is not a function`
     - `InterviewHistoryView.vue: TypeError: (res.data || []).sort is not a function`

  2. 后端日志显示面试历史接口已使用分页参数：
     - `获取面试历史, userId: xxx, pageNum: 1, pageSize: 10`

  3. 当前数据库真实结构：
     - `interview_session` 包含：
       - `session_id`
       - `user_id`
       - `job_role`
       - `difficulty`
       - `status`
       - `comprehensive_score`
       - `evaluation_report`
       - `create_time`
       - `update_time`
       - `is_deleted`
     - `interview_chat_log` 可用于统计消息数
     - `resume_diagnosis_task` 真实字段不包含：
       - `task_id`
       - `result`
       - `error_message`

  4. 扩展字段说明：
     - 如果后端当前已经返回 `interviewMode` / `interviewModeDesc` / `messageCount`，前端直接使用
     - 如果后端尚未稳定返回这些字段，前端必须兜底显示，不能报错
     - `messageCount` 可能来自后端聚合统计，不应由前端假设固定结构
     - `interviewMode` 如果是正式业务属性，后续可能需要真正落库；但本任务先做前端适配与容错，不擅自改后端协议

  **任务要求：​**

  #### 1. 先确认接口真实返回结构
  - 检查前端请求面试历史接口时实际拿到的响应结构
  - 确认真正的历史数组位于哪里，例如：
    - `res.data.records`
    - `res.data.list`
    - `res.data.data.records`
    - 或其他真实路径
  - 不要凭经验直接写死结构，先打印并核对真实响应

  #### 2. 修复分页响应适配
  - `HomeView.vue` 和 `InterviewHistoryView.vue` 中，不要再把 `res.data` 直接当数组
  - 从真实分页对象中提取列表数组
  - 同时正确接收分页信息：
    - `total`
    - `pageNum`
    - `pageSize`

  #### 3. 排序前必须做数组保护
  - 任何 `.sort()` 调用之前，必须先确保目标是数组
  - 例如逻辑上要做到：
    - 如果是数组才排序
    - 如果不是数组则回退为空数组
  - 不能再次出现 `sort is not a function`

  #### 4. 历史字段显示与当前后端真实字段对齐
  前端展示请优先使用当前真实字段，不再继续假设旧字段或不存在字段。  
  面试历史页面重点对齐这些字段：

  - `sessionId`
  - `jobRole`
  - `difficulty`
  - `status`
  - `createTime`
  - `comprehensiveScore`
  - `evaluationReport`

  如果后端已返回扩展字段，也同步接入：

  - `interviewMode`
  - `interviewModeDesc`
  - `messageCount`

  #### 5. 对扩展字段做兜底显示
  - `interviewModeDesc` 不存在时，不要报错，可显示：
    - `普通面试`
    - 或 `未设置`
  - `messageCount` 不存在时，不要报错，可显示：
    - `0`
    - 或 `--`
  - 所有字段访问都避免空值引发运行时异常

  #### 6. 同步修复两个页面
  至少修改以下文件：

  - `frontend/src/views/HomeView.vue`
  - `frontend/src/views/InterviewHistoryView.vue`

  这两个页面的历史列表逻辑都需要统一处理，避免一个修好另一个仍然报错。

  #### 7. 不修改后端接口，不新增假设字段
  - 本任务仅做前端适配与容错
  - 不要为了凑字段去修改后端接口协议
  - 不要在前端写死不存在的字段来源
  - 所有逻辑以当前后端真实返回为准

  **输出要求：​**
  1. 根因说明
  2. 当前接口真实响应结构说明
  3. 修改文件清单
  4. `HomeView.vue` 关键修改代码
  5. `InterviewHistoryView.vue` 关键修改代码
  6. 说明如何保证：
     - 分页结构兼容
     - 历史字段显示一致
     - 面试模式/消息数字段缺失时不会报错

  **执行提醒：​**
  先打印并核对接口真实响应结构，再改代码；不要未经验证直接写死 `records` 或 `list`。