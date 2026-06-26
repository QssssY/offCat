import { defaultResumeData } from './_default.js'
export default { ...defaultResumeData }

export const defaultSectionsConfig = [
  { key: 'summary', title: '个人简介', visible: false, type: 'summary' },
  { key: 'skills', title: '专业技能', visible: false, type: 'skills' },
  { key: 'education', title: '教育经历', visible: true, type: 'experience' },
  { key: 'work', title: '工作经历', visible: true, type: 'experience' },
  { key: 'projects', title: '项目经历', visible: true, type: 'experience' }
]
