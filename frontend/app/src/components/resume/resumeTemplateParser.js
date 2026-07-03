const generateBlockId = (prefix = 'resume_block') => `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

const SECTION_SPECS = [
  {
    key: 'profile',
    title: '个人信息',
    aliases: ['个人信息', '基本信息', '求职意向', '求职方向', '个人概况', '个人资料'],
  },
  {
    key: 'education',
    title: '教育背景',
    aliases: ['教育背景', '教育经历', '学历背景', '院校经历'],
  },
  {
    key: 'experience',
    title: '实习经历',
    aliases: ['实习经历', '工作经历', '工作经验', '职业经历', '实践经历', '实习经验'],
  },
  {
    key: 'project',
    title: '项目经历',
    aliases: ['项目经历', '项目经验', '科研项目', '实践项目'],
  },
  {
    key: 'skill',
    title: '专业技能',
    aliases: ['专业技能', '专业能力', '职业技能', '核心技能', '技能特长', '技能清单'],
  },
  {
    key: 'campus',
    title: '校园经历',
    aliases: ['校园经历', '校内经历', '学生工作', '社团经历', '组织经历', '教育与实践经历'],
  },
  {
    key: 'honor',
    title: '荣誉证书',
    aliases: ['荣誉证书', '荣誉奖项', '证书资质', '技术资质', '技能证书', '获奖情况', '荣誉奖励'],
  },
  {
    key: 'evaluation',
    title: '个人评价',
    aliases: ['个人评价', '自我评价', '职业优势', '个人优势', '个人总结', '个人陈述', '专业能力总结'],
  },
]

const DISPLAY_ORDER = ['education', 'experience', 'project', 'skill', 'campus', 'honor', 'evaluation']
const RESUME_START_KEYS = new Set(['profile', 'education', 'experience', 'project', 'skill', 'honor', 'evaluation'])
const EDUCATION_DEGREE_TOKENS = ['博士', '硕士', '研究生', '本科', '专科', '大专', '中专', '高中']
const EDUCATION_STATUS_TOKENS = ['大一', '大二', '大三', '大四', '在读', '应届']
const CERTIFICATE_KEYWORDS = ['证书', '资格证', '计算机二级', '程序员', '软考', '英语四级', '英语六级', '普通话']
const DATE_RANGE_SOURCE = String.raw`\d{4}[年./-]\d{1,2}[月]?\s*[-~至]\s*(?:\d{4}[年./-]\d{1,2}[月]?|至今|现在)`
const DATE_RANGE_PATTERN = new RegExp(`^${DATE_RANGE_SOURCE}$`)
const DATE_RANGE_TAIL_PATTERN = new RegExp(`[（(]?(${DATE_RANGE_SOURCE})[）)]?$`)
const HEADER_META_LABEL_PATTERN =
  /(?:\s+|\s*[|｜]\s*)((?:姓名|性别|年龄|联系电话|电话|手机|电子邮箱|邮箱|求职意向|期望薪资|现居地|现居城市|地址|微信|住址|政治面貌|紧急联系人)[：:])/g
const DETAIL_LABEL_PATTERN = /\s+((?:在校成绩|核心课程|项目描述|负责内容|项目成果|联系方式|备注)[：:])/g

function escapeRegExp(value) {
  return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

const SECTION_ALIAS_LOOKUP = SECTION_SPECS.flatMap((spec) => spec.aliases.map((alias) => ({ key: spec.key, alias })))
const SECTION_TITLE_WITH_COLON_PATTERN = new RegExp(
  `(${SECTION_ALIAS_LOOKUP.map(({ alias }) => escapeRegExp(alias)).sort((a, b) => b.length - a.length).join('|')})\\s*[：:]\\s*`,
  'g',
)
const NUMBERED_SECTION_PATTERN = /([一二三四五六七八九十]+[、.]?\s*(?:专业技能|项目经验|项目经历|荣誉证书|自我评价|教育与实践经历|教育背景|校园经历))/g
const TITLE_GARBAGE_PREFIX_PATTERN = /^[【\[(（\s]*/
const TITLE_GARBAGE_SUFFIX_PATTERN = /[】\])）:\s：]+$/
const TRAILING_METADATA_START_PATTERN = /\s*(?:\(String\)|\(LocalDateTime\)|\(Integer\)|<==\s*Updates:|仅基于简历\(String\)|\[(?=")|\{(?="))/u

const EDUCATION_ROW_COMPACT_PATTERN = new RegExp(
  `^(.+?)\\s+(\\S+)\\s+(\\u535A\\u58EB|\\u7855\\u58EB|\\u7814\\u7A76\\u751F|\\u672C\\u79D1|\\u4E13\\u79D1|\\u5927\\u4E13|\\u4E2D\\u4E13|\\u9AD8\\u4E2D|\\u5927\\u4E00|\\u5927\\u4E8C|\\u5927\\u4E09|\\u5927\\u56DB|\\u5728\\u8BFB|\\u5E94\\u5C4A)\\s*[\\uFF08(]?(${DATE_RANGE_SOURCE})[\\uFF09)]?$`,
)
const TRAILING_METADATA_LINE_PATTERNS = [
  /^\[(?=")/,
  /^\{(?=")/,
  /\u4EC5\u57FA\u4E8E\u7B80\u5386\(String\)/,
  /\(LocalDateTime\)/,
  /\(Integer\)/,
  /\(String\)\s*,/,
  /^<==\s*Updates:/i,
]

const escapeHtml = (value) => {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const wrapParagraphHtml = (value) => {
  return `<p>${escapeHtml(value).replace(/\n/g, '<br>')}</p>`
}

const uniquePush = (target, value, maxLength = 8) => {
  if (!value || target.includes(value) || target.length >= maxLength) {
    return
  }
  target.push(value)
}

const createStyleModel = () => ({
  fontSize: null,
  fontWeight: null,
})

const createTextBlock = (content, variant = '') => ({
  id: generateBlockId(),
  type: 'text',
  variant,
  html: wrapParagraphHtml(content),
  style: createStyleModel(),
})

const createSectionTitleBlock = (content) => ({
  id: generateBlockId(),
  type: 'section_title',
  html: wrapParagraphHtml(content),
  style: createStyleModel(),
})

const createLabelBlock = (label, value) => ({
  id: generateBlockId(),
  type: 'label',
  label,
  value,
  style: createStyleModel(),
})

const createRowBlock = (items, rowKind = 'default') => ({
  id: generateBlockId(),
  type: 'row',
  rowKind,
  items: items.map((item) => ({
    id: generateBlockId(),
    value: item,
  })),
  style: createStyleModel(),
})

const normalizeTitleText = (line) => {
  return String(line || '')
    .replace(TITLE_GARBAGE_PREFIX_PATTERN, '')
    .replace(TITLE_GARBAGE_SUFFIX_PATTERN, '')
    .replace(/^[0-9一二三四五六七八九十]+[.)、\s]*/, '')
    .trim()
}

const findSectionSpecByKey = (key) => SECTION_SPECS.find((item) => item.key === key)

const matchSectionHeader = (line) => {
  const normalized = normalizeTitleText(line)
  if (!normalized) {
    return null
  }

  for (const spec of SECTION_SPECS) {
    const matchedAlias = spec.aliases.find((alias) => normalized === alias || normalized.startsWith(alias))
    if (matchedAlias) {
      const suffix = normalized.slice(matchedAlias.length)
      if (suffix && !/^[与及、\/\s(（:：-]/.test(suffix)) {
        continue
      }
      return {
        key: spec.key,
        rawTitle: normalized,
      }
    }
  }

  return null
}

const stripTrailingMetadataInline = (line) => {
  const normalized = String(line || '')
  const match = normalized.match(TRAILING_METADATA_START_PATTERN)
  if (!match?.index) {
    return normalized.trim()
  }
  return normalized.slice(0, match.index).trim().replace(/[，,;；]+$/, '').trim()
}

const preprocessInlineSectionHeaders = (text) => {
  return String(text || '')
    .normalize('NFKC')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/\uFEFF/g, '')
    .replace(NUMBERED_SECTION_PATTERN, '\n$1\n')
    .replace(SECTION_TITLE_WITH_COLON_PATTERN, '\n$1\n')
    .replace(/(?:^|\s)项目\d+[：:]\s*/g, '\n')
    .replace(HEADER_META_LABEL_PATTERN, '\n$1')
    .replace(DETAIL_LABEL_PATTERN, '\n$1')
    .replace(/\s+((?:[0-9]+[.)、]\s*(?:校内实践|课程实践)|[（(][0-9]+[）)]))/g, '\n$1')
    .replace(/;\s*(?=\d+[.)、])/g, ';\n')
    .replace(/\s+•\s*/g, '\n• ')
    .replace(/\n{3,}/g, '\n\n')
}

const normalizeInputLines = (text) => {
  const normalized = preprocessInlineSectionHeaders(text)
  const rawLines = normalized.split('\n')
  const lines = []
  let skipSummary = false

  rawLines.forEach((rawLine) => {
    const line = stripTrailingMetadataInline(rawLine)
    if (!line) {
      return
    }

    const header = matchSectionHeader(line)
    if (skipSummary && header) {
      skipSummary = false
    }

    if (/^摘\s*要(?:[:：].*)?$/u.test(line)) {
      skipSummary = true
      return
    }

    if (skipSummary) {
      return
    }

    if (/^(AI润色简历|个人简历|求职简历|简历)$/.test(line)) {
      return
    }

    if (/^(备注|注)[:：]/.test(line) || /^[（(]注[:：]/.test(line)) {
      return
    }

    if (/^[0-9一二三四五六七八九十]+[.)、]?$/.test(line)) {
      return
    }

    lines.push(line)
  })

  return lines
}

const splitInlineItems = (line) => {
  return String(line || '')
    .split(/\s*(?:\||\uFF5C|\u00B7|\u2022)\s*/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const isInlineRow = (line) => splitInlineItems(line).length >= 2

const isLabelLine = (line) => /^[^：:\n]{2,12}[：:]\s*.+$/.test(line)

const parseLabelLine = (line) => {
  const colonIndex = line.indexOf('：')
  const asciiIndex = line.indexOf(':')
  const separatorIndex =
    colonIndex === -1 ? asciiIndex : asciiIndex === -1 ? colonIndex : Math.min(colonIndex, asciiIndex)

  if (separatorIndex < 0) {
    return {
      label: '',
      content: line,
    }
  }

  return {
    label: line.slice(0, separatorIndex + 1).trim(),
    content: line.slice(separatorIndex + 1).trim(),
  }
}

const isListItem = (line) => /^[-*•·]\s*/.test(line) || /^\d+[.)、\s]/.test(line)

const normalizeListItem = (line) => {
  return String(line || '')
    .replace(/^[-*•·]\s*/, '')
    .replace(/^\d+[.)、]\s*/, '')
    .trim()
}

const isContactLikeLine = (line) => {
  return /@|电话|手机|邮箱|微信|地址|现居|居住|城市|GitHub|Github|博客|LinkedIn|出生|年龄|政治面貌|\d{7,}/.test(line)
}

const isJobTargetLabel = (label) => /求职|应聘|方向|意向|岗位|目标/.test(label)

const isLikelyName = (line) => {
  if (!line || line.length > 24 || /\d{4}/.test(line)) {
    return false
  }
  if (isInlineRow(line) || isLabelLine(line)) {
    return false
  }
  return /^[A-Za-z\u4e00-\u9fa5·\s]{2,24}$/.test(line)
}

const extractDisplayName = (line) => {
  const normalizedLine = String(line || '').trim()
  if (!normalizedLine) {
    return ''
  }

  const labelMatch = normalizedLine.match(/姓名[:：]\s*([\u4e00-\u9fa5A-Za-z·]{2,24})/)
  if (labelMatch?.[1]) {
    return labelMatch[1].trim()
  }

  const match = normalizedLine.match(/(?:简历|个人简历)\s*[-—]?\s*([\u4e00-\u9fa5A-Za-z·]{2,24})$/)
  if (match?.[1]) {
    return match[1].trim()
  }

  const leadingMatch = normalizedLine.match(/^([\u4e00-\u9fa5A-Za-z·]{2,24})\s*[-—]\s*.+简历$/)
  if (leadingMatch?.[1]) {
    return leadingMatch[1].trim()
  }

  if (isLikelyName(normalizedLine)) {
    return normalizedLine
  }

  return ''
}

const shouldEmphasizeProjectLine = (sectionKey, line) => {
  if (sectionKey !== 'project') {
    return false
  }
  if (!line || isLabelLine(line) || isInlineRow(line)) {
    return false
  }
  if (/[：:。；;]/.test(line)) {
    return false
  }
  return line.length <= 32 || /\d{4}[./-]\d{1,2}/.test(line)
}

const shouldUseSubTitleBlock = (line, nextLine) => {
  if (!line || line.length > 20) {
    return false
  }
  if (isInlineRow(line) || isLabelLine(line) || isListItem(line)) {
    return false
  }
  if (/[：:。；;，,]/.test(line)) {
    return false
  }
  if (!nextLine) {
    return false
  }
  return isInlineRow(nextLine) || isLabelLine(nextLine) || nextLine.length >= line.length
}

const resolveSectionTitle = (key, sourceTitles) => {
  if (key !== 'experience') {
    return findSectionSpecByKey(key)?.title || '简历内容'
  }

  if (sourceTitles.includes('工作经历') || sourceTitles.includes('工作经验')) {
    return '工作经历'
  }

  return '实习经历'
}

const isEducationDate = (token) => DATE_RANGE_PATTERN.test(String(token || '').trim())

const normalizeEducationToken = (value) => String(value || '').replace(/\s+/g, ' ').trim()

const parseEducationRow = (line) => {
  if (!line || isLabelLine(line) || isListItem(line)) {
    return null
  }

  const inlineItems = splitInlineItems(line)
  if (inlineItems.length === 4 && inlineItems.some(isEducationDate)) {
    return inlineItems.map(normalizeEducationToken)
  }

  const compactMatch = line.match(EDUCATION_ROW_COMPACT_PATTERN)
  if (compactMatch) {
    return [compactMatch[1], compactMatch[2], compactMatch[3], compactMatch[4]].map(normalizeEducationToken)
  }

  const dateMatch = line.match(DATE_RANGE_TAIL_PATTERN)
  if (!dateMatch) {
    return null
  }

  const date = normalizeEducationToken(dateMatch[1])
  const prefix = line
    .slice(0, dateMatch.index)
    .trim()
    .replace(/[（(\s]+$/, '')
    .trim()
  const thirdToken =
    [...EDUCATION_DEGREE_TOKENS, ...EDUCATION_STATUS_TOKENS].find((item) => prefix.endsWith(item)) || ''
  if (!thirdToken) {
    return null
  }

  const head = prefix.slice(0, -thirdToken.length).trim()
  const headTokens = head.split(/\s+/).filter(Boolean)
  if (headTokens.length >= 2) {
    const major = headTokens.pop()
    const school = headTokens.join(' ')
    if (school && major) {
      return [school, major, thirdToken, date]
    }
  }

  const compactMajorMatch = head.match(/^(.+?(?:大学|学院|学校))(.+?专业)$/)
  if (compactMajorMatch) {
    return [compactMajorMatch[1], compactMajorMatch[2], thirdToken, date].map(normalizeEducationToken)
  }

  return null
}

const normalizeEntryToken = (value) => String(value || '').replace(/\s+/g, ' ').trim()

const parseExperienceOrProjectRow = (line, allowRoleless = false) => {
  if (!line || isLabelLine(line) || isListItem(line)) {
    return null
  }

  const inlineItems = splitInlineItems(line)
  if (inlineItems.length === 3 && isEducationDate(inlineItems[2])) {
    return inlineItems.map(normalizeEntryToken)
  }

  // 处理 campus 格式："角色 | 2024.09-至今:描述" 或 "角色 | 2024.09-至今"
  if (inlineItems.length >= 2) {
    const lastItem = inlineItems[inlineItems.length - 1]
    const innerDateMatch = lastItem.match(DATE_RANGE_TAIL_PATTERN)
    if (innerDateMatch) {
      const date = normalizeEntryToken(innerDateMatch[1])
      const title = inlineItems.length > 2 ? inlineItems[1] : inlineItems[0]
      return [normalizeEntryToken(title), '', date]
    }
    // 日期后跟冒号："2024.09-至今:负责..."
    const colonDateMatch = lastItem.match(
      /([（(]?\d{4}[年.\-]\d{1,2}[月]?\s*[-~至]\s*\d{4}[年./-]\d{1,2}[月]?[）)]?)\s*[:：]/
    )
    if (colonDateMatch) {
      const date = normalizeEntryToken(colonDateMatch[1])
      const title = inlineItems.length > 2 ? inlineItems[1] : inlineItems[0]
      return [normalizeEntryToken(title), '', date]
    }
  }

  const dateMatch = line.match(DATE_RANGE_TAIL_PATTERN)
  if (!dateMatch) {
    return null
  }

  const date = normalizeEntryToken(dateMatch[1])
  const prefix = line
    .slice(0, dateMatch.index)
    .trim()
    .replace(/[（(\s]+$/, '')
    .trim()
  if (!prefix) {
    return null
  }

  const parts = prefix.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) {
    const role = parts.pop()
    const title = parts.join(' ')
    if (title && role) {
      return [title, role, date]
    }
  }

  if (allowRoleless) {
    return [prefix, '', date]
  }

  return null
}

const isCertificateLikeItem = (line) => {
  const normalized = normalizeListItem(line)
  return CERTIFICATE_KEYWORDS.some((keyword) => normalized.includes(keyword))
}

const splitHonorItems = (line) => {
  return String(line || '')
    .split(/\s*(?:\||｜|、|，|,)\s*/)
    .map((item) => normalizeListItem(item))
    .filter(Boolean)
}

/** 解析校园经历行，返回 { row: [title, '', date], descriptions: string[] } 或 null */
const parseCampusLine = (line) => {
  if (!line || isLabelLine(line) || isListItem(line)) return null

  const inlineItems = splitInlineItems(line)
  if (inlineItems.length >= 2) {
    // 格式："角色 | 2024.09-至今:描述内容"
    const lastItem = inlineItems[inlineItems.length - 1]
    const colonMatch = lastItem.match(
      /([（(]?\d{4}[年.\-]\d{1,2}[月]?\s*[-~至]\s*(?:\d{4}[年./-]\d{1,2}[月]?|至今|现在)[）)]?)\s*[:：]\s*([\s\S]*)/
    )
    if (colonMatch) {
      const title = inlineItems.length > 2 ? inlineItems[1] : inlineItems[0]
      const date = normalizeEntryToken(colonMatch[1])
      const desc = colonMatch[2].trim()
      const result = { row: [normalizeEntryToken(title), '', date], descriptions: [] }
      if (desc) result.descriptions.push(desc)
      return result
    }
    // 格式："角色 | 2024.09-至今"（无描述）
    const dateOnlyMatch = lastItem.match(DATE_RANGE_TAIL_PATTERN)
    if (dateOnlyMatch) {
      const title = inlineItems.length > 2 ? inlineItems[1] : inlineItems[0]
      return { row: [normalizeEntryToken(title), '', normalizeEntryToken(dateOnlyMatch[1])], descriptions: [] }
    }
  }

  // 格式："标题:内容"（无日期，如"课程实践:完成..."）
  const colonParts = line.match(/^(.+?)[:：]\s*([\s\S]+)$/)
  if (colonParts) {
    const title = colonParts[1].trim()
    const desc = colonParts[2].trim()
    if (title && desc) {
      return { row: [title, '', ''], descriptions: [desc] }
    }
  }

  return null
}

const createEmptySectionBucket = () => ({
  sourceTitles: [],
  rawLines: [],
})

const looksLikeTrailingMetadata = (line) => {
  const normalized = String(line || '').trim()
  if (!normalized) {
    return false
  }
  return TRAILING_METADATA_LINE_PATTERNS.some((pattern) => pattern.test(normalized))
}

const isResumeContactLikeRow = (line) => {
  if (!isInlineRow(line)) {
    return false
  }
  return /@|电话|手机|邮箱|微信|地址|现居|城市|github|linkedin|\d{7,}/i.test(line)
}

const isResumeIdentityLabelLine = (line) => {
  if (!isLabelLine(line)) {
    return false
  }
  const cleanLabel = parseLabelLine(line).label.replace(/[：:]/g, '')
  return /^(姓名|电话|手机|联系电话|邮箱|电子邮箱|微信|地址|现居地|现居城市|求职意向|期望薪资)$/.test(cleanLabel)
}

const isLikelyJobTitleLine = (line) => {
  if (!line || line.length > 32 || isInlineRow(line) || isLabelLine(line) || /\d{4}/.test(line)) {
    return false
  }
  return /(工程师|开发|设计|产品|运营|测试|分析|专员|实习生|求职)/.test(line)
}

const looksLikeResumeRestartLine = (line, nextLine = '') => {
  if (!line) {
    return false
  }
  if (isLikelyName(line) || isResumeContactLikeRow(line) || isResumeIdentityLabelLine(line)) {
    return true
  }
  if (isContactLikeLine(line) && line.length <= 40) {
    return true
  }
  if (!isLikelyJobTitleLine(line)) {
    return false
  }
  return isResumeContactLikeRow(nextLine) || isResumeIdentityLabelLine(nextLine) || isContactLikeLine(nextLine)
}

const collectSections = (lines) => {
  const leadLines = []
  const sectionMap = new Map()
  let currentSectionKey = null
  let startedResumeBody = false

  const ensureSection = (key, rawTitle = '') => {
    if (!sectionMap.has(key)) {
      sectionMap.set(key, createEmptySectionBucket())
    }
    const section = sectionMap.get(key)
    if (rawTitle && !section.sourceTitles.includes(rawTitle)) {
      section.sourceTitles.push(rawTitle)
    }
    return section
  }

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    const nextLine = lines[index + 1] || ''

    if (looksLikeTrailingMetadata(line)) {
      break
    }

    const header = matchSectionHeader(line)
    if (header) {
      if (startedResumeBody && RESUME_START_KEYS.has(header.key) && sectionMap.has(header.key) && header.key !== currentSectionKey) {
        // 同一 key 的别名（如"实习经历"→"工作经历"）不应中断解析，仅在遇到不同 key 且当前章节为空时中断。
        const currentSection = currentSectionKey ? sectionMap.get(currentSectionKey) : null
        if (!currentSection || !currentSection.rawLines.length) {
          break
        }
      }
      currentSectionKey = header.key
      startedResumeBody = startedResumeBody || RESUME_START_KEYS.has(header.key)
      ensureSection(header.key, header.rawTitle)
      continue
    }

    if (!currentSectionKey) {
      leadLines.push(line)
      continue
    }

    if (
      sectionMap.has('profile')
      && (currentSectionKey === 'evaluation' || currentSectionKey === 'honor')
      && looksLikeResumeRestartLine(line, nextLine)
    ) {
      break
    }

    ensureSection(currentSectionKey).rawLines.push(line)
  }

  return {
    leadLines,
    sectionMap,
  }
}

const migrateSkillCertificates = (sectionMap) => {
  const skillSection = sectionMap.get('skill')
  if (!skillSection?.rawLines?.length) {
    return
  }

  const preservedSkillLines = []
  const migratedHonorLines = []

  skillSection.rawLines.forEach((line) => {
    if (isCertificateLikeItem(line)) {
      migratedHonorLines.push(...splitHonorItems(line))
      return
    }
    preservedSkillLines.push(line)
  })

  skillSection.rawLines = preservedSkillLines

  if (!migratedHonorLines.length) {
    return
  }

  const honorSection = sectionMap.get('honor') || createEmptySectionBucket()
  if (!honorSection.sourceTitles.length) {
    honorSection.sourceTitles.push(findSectionSpecByKey('honor')?.title || '荣誉证书')
  }

  const mergedHonorLines = [...honorSection.rawLines]
  migratedHonorLines.forEach((item) => uniquePush(mergedHonorLines, item, 99))
  honorSection.rawLines = mergedHonorLines
  sectionMap.set('honor', honorSection)
}

const buildBlocksFromLines = (sectionKey, lines) => {
  const blocks = []

  if (sectionKey === 'honor') {
    lines.forEach((line) => {
      splitHonorItems(line).forEach((item) => {
        blocks.push(createTextBlock(item, 'bullet'))
      })
    })
    return blocks
  }

  lines.forEach((line, index) => {
    const nextLine = lines[index + 1] || ''

    if (sectionKey === 'education') {
      const educationItems = parseEducationRow(line)
      if (educationItems) {
        blocks.push(createRowBlock(educationItems, 'education'))
        return
      }
    }

    if (sectionKey === 'campus') {
      // 校园经历格式："角色 | 2024.09-至今:描述内容"
      // 解析为 header row (role + date) + bullet text blocks (description)
      const campusResult = parseCampusLine(line)
      if (campusResult) {
        blocks.push(createRowBlock(campusResult.row))
        campusResult.descriptions.forEach((desc) => {
          blocks.push(createTextBlock(desc, 'bullet'))
        })
        return
      }
    }

    if (sectionKey === 'experience') {
      const experienceItems = parseExperienceOrProjectRow(line)
      if (experienceItems) {
        blocks.push(createRowBlock(experienceItems))
        return
      }
    }

    if (sectionKey === 'project') {
      // 项目描述中分号分隔不同要点，拆分为独立 bullet 行
      const subLines = line.split(/;\s*/).filter(Boolean)
      if (subLines.length > 1) {
        subLines.forEach((sub) => {
          blocks.push(createTextBlock(sub.trim(), 'bullet'))
        })
        return
      }
      const projectItems = parseExperienceOrProjectRow(line, true)
      if (projectItems) {
        blocks.push(createRowBlock(projectItems))
        return
      }
    }

    if (isListItem(line)) {
      const item = normalizeListItem(line)
      if (item) {
        blocks.push(createTextBlock(item, 'bullet'))
      }
      return
    }

    if (shouldUseSubTitleBlock(line, nextLine)) {
      blocks.push(createSectionTitleBlock(line))
      return
    }

    if (isInlineRow(line)) {
      blocks.push(createRowBlock(splitInlineItems(line)))
      return
    }

    if (isLabelLine(line)) {
      const parsed = parseLabelLine(line)
      blocks.push(createLabelBlock(parsed.label, parsed.content))
      return
    }

    blocks.push(createTextBlock(line, shouldEmphasizeProjectLine(sectionKey, line) ? 'heading' : ''))
  })

  return blocks
}

const buildProfileHeader = (leadLines, profileLines, sourceTitles = []) => {
  const sourceLines = [...leadLines, ...profileLines]
  const metaItems = []
  let name = ''
  let jobTarget = ''

  // 当 "求职意向" 被 SECTION_TITLE_WITH_COLON_PATTERN 拆分为独立章节标题时，
  // 它被 collectSections 作为 section header 消费，不会出现在 profileLines 中。
  // 此时通过 sourceTitles 检测，并从 profileLines 中提取求职意向值。
  // 值可能与住址等字段混合在同一行（如 "软件测试工程师(初级) 住址:..."），需截取到第一个字段标签前。
  if (sourceTitles.some(t => /^求职意向$/.test(t)) && !jobTarget) {
    const PROFILE_FIELD_LABELS = /^(姓名|性别|年龄|联系电话|电话|手机|电子邮箱|邮箱|住址|地址|政治面貌|紧急联系人|现居地|现居城市|微信|GitHub|Github|博客)/
    for (const pl of profileLines) {
      if (!pl) continue
      const segments = pl.split(/\s+/)
      const valueSegments = []
      for (const seg of segments) {
        if (PROFILE_FIELD_LABELS.test(seg) || seg === '|' || seg === '｜') break
        valueSegments.push(seg)
      }
      const candidate = valueSegments.join(' ').trim()
      if (candidate.length >= 2) {
        jobTarget = candidate
        break
      }
    }
  }

  sourceLines.forEach((line, index) => {
    if (!name && index === 0) {
      const displayName = extractDisplayName(line)
      if (displayName) {
        name = displayName
        return
      }
    }

    if (!name && isLikelyName(line)) {
      name = line
      return
    }

    if (isLabelLine(line)) {
      const parsed = parseLabelLine(line)
      const cleanLabel = parsed.label.replace(/[：:]/g, '')

      if (cleanLabel === '姓名') {
        if (!name && parsed.content) {
          name = parsed.content
        }
        return
      }

      if (!jobTarget && isJobTargetLabel(cleanLabel)) {
        jobTarget = parsed.content
        return
      }

      if (/^摘要$/.test(cleanLabel)) {
        return
      }

      if (/^(政治面貌|紧急联系人)$/.test(cleanLabel)) {
        return
      }

      if (isContactLikeLine(line) || parsed.content.length <= 24) {
        uniquePush(metaItems, `${cleanLabel}：${parsed.content}`)
        return
      }
    }

    if (isInlineRow(line)) {
      splitInlineItems(line).forEach((item) => {
        const inlineName = extractDisplayName(item)
        if (!name && inlineName) {
          name = inlineName
          return
        }
        if (/^姓名[:：]/.test(item)) {
          if (!name) {
            name = item.replace(/^姓名[:：]\s*/, '').trim()
          }
          return
        }
        if (!jobTarget && isJobTargetLabel(item)) {
          jobTarget = item
          return
        }
        uniquePush(metaItems, item)
      })
      return
    }

    if (!jobTarget && index <= 2 && line.length <= 20 && !isContactLikeLine(line)) {
      jobTarget = line
      return
    }

    if (jobTarget && line === jobTarget) {
      return
    }

    if (isContactLikeLine(line) || line.length <= 22) {
      uniquePush(metaItems, line)
    }
  })

  return {
    sectionTitle: '个人信息',
    name,
    jobTarget,
    metaItems: metaItems.map((item) => ({
      id: generateBlockId(),
      value: item,
    })),
    summaryLines: [],
  }
}

/**
 * 将 AI 返回的纯文本简历解析为前端模板使用的数据模型。
 * 这里统一处理章节识别、重复正文截断、技能/证书纠偏以及评价章节独立保留。
 */
export const buildResumeTemplateModel = (text) => {
  const lines = normalizeInputLines(text)
  const { leadLines, sectionMap } = collectSections(lines)

  migrateSkillCertificates(sectionMap)

  const profileSection = sectionMap.get('profile')
  const header = buildProfileHeader(leadLines, profileSection?.rawLines || [], profileSection?.sourceTitles || [])
  const sections = []

  DISPLAY_ORDER.forEach((key) => {
    const sourceSection = sectionMap.get(key)
    if (!sourceSection || !sourceSection.rawLines.length) {
      return
    }

    sections.push({
      id: generateBlockId(),
      key,
      title: resolveSectionTitle(key, sourceSection.sourceTitles),
      blocks: buildBlocksFromLines(key, sourceSection.rawLines),
    })
  })

  if (!sections.length) {
    sections.push({
      id: generateBlockId(),
      key: 'content',
      title: '简历内容',
      blocks: leadLines.length ? buildBlocksFromLines('content', leadLines) : [createTextBlock('请在这里补充简历内容')],
    })
  }

  return {
    header,
    sections,
  }
}

export const createEmptyTextBlock = () => createTextBlock('')

export const createEmptySectionTitleBlock = () => createSectionTitleBlock('请输入小标题')

export const createEmptyLabelBlock = () => createLabelBlock('标签：', '请输入内容')
