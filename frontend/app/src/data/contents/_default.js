// 所有模板共享的默认简历内容（示例数据）
export const defaultResumeData = {
  basic: {
    name: '张三',
    title: '高级前端工程师',
    phone: '138-0000-0000',
    email: 'zhangsan@example.com',
    location: '北京市',
    website: 'https://zhangsan.dev',
    photo: ''
  },
  summary: '5年前端开发经验，精通 Vue.js 和 React 生态。具备大型项目架构设计能力，注重代码质量和团队协作。曾主导多个核心产品的前端重构，性能优化成果显著。',
  skills: [
    '熟练掌握 JavaScript / TypeScript，深入理解原型链、闭包、异步编程等核心机制',
    '精通 Vue.js 全家桶，具备大型 SPA 架构设计与性能优化经验',
    '熟悉 React 技术栈，能够独立完成组件开发与状态管理方案设计',
    '掌握 Node.js 后端开发，有 Express / Koa 实战经验',
    '熟悉 Webpack / Vite 构建工具，能独立完成工程化配置与优化',
    '熟练使用 Git 进行版本管理与团队协作',
    '具备良好的编码规范与 Code Review 意识'
  ],
  education: [
    {
      id: 'edu-1',
      school: '北京大学',
      degree: '本科',
      major: '计算机科学与技术',
      startDate: '2015-09',
      endDate: '2019-06',
      description: 'GPA 3.8/4.0，获校级奖学金'
    }
  ],
  work: [
    {
      id: 'work-1',
      company: '某科技有限公司',
      position: '高级前端工程师',
      startDate: '2021-03',
      endDate: '至今',
      highlights: [
        '主导公司核心产品的前端架构升级，从 Vue 2 迁移到 Vue 3',
        '性能优化使首屏加载时间减少 40%，用户留存率提升 15%',
        '建立前端代码规范和 Code Review 流程，提升团队代码质量'
      ]
    },
    {
      id: 'work-2',
      company: '某互联网公司',
      position: '前端工程师',
      startDate: '2019-07',
      endDate: '2021-02',
      highlights: [
        '负责电商平台的前端开发和维护',
        '实现组件库搭建，提升团队开发效率 30%'
      ]
    }
  ],
  projects: [
    {
      id: 'proj-1',
      name: '智能简历诊断系统',
      role: '前端负责人',
      startDate: '2023-01',
      endDate: '2023-06',
      description: '基于 Vue 3 + Tiptap 构建的简历在线编辑与诊断平台',
      highlights: [
        '设计并实现了模块化简历编辑器，支持拖拽排序和富文本编辑',
        '接入 AI 引擎实现智能诊断，日活用户超过 5000'
      ]
    },
    {
      id: 'proj-2',
      name: '企业级数据看板',
      role: '核心开发者',
      startDate: '2022-03',
      endDate: '2022-09',
      description: '面向企业客户的实时数据可视化平台',
      highlights: [
        '使用 ECharts 和 Canvas 实现多种图表组件',
        '支持实时数据推送和大屏展示模式'
      ]
    }
  ],
  // 以下为可选扩展字段，供用户自行新增段落时使用
  certifications: [],
  awards: [],
  languages: [],
  interests: []
}
