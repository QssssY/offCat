<template>
  <main class="offer-page">
    <section class="offer-hero" aria-labelledby="offer-title">
      <div class="hero-copy">
        <span class="eyebrow">Offer assistant</span>
        <h1 id="offer-title">Offer 辅助</h1>
        <p>把谈薪场景拆成可执行的回复、策略和收口动作。</p>
      </div>
      <div class="hero-note">
        <span class="note-label">当前范围</span>
        <span>薪资谈判模拟 / 谈薪话术模板</span>
      </div>
    </section>

    <section class="offer-shell" aria-label="Offer 辅助工作台">
      <div class="mode-switch" role="tablist" aria-label="Offer 辅助类型">
        <button
          v-for="item in modeOptions"
          :key="item.value"
          type="button"
          class="mode-button"
          :class="{ active: activeTab === item.value }"
          role="tab"
          :aria-selected="activeTab === item.value"
          @click="activeTab = item.value"
        >
          <span class="mode-icon" v-html="item.icon"></span>
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.desc }}</small>
          </span>
        </button>
      </div>

      <div class="workbench">
        <section class="input-panel" aria-label="输入谈薪信息">
          <header class="panel-head">
            <div>
              <h2>{{ currentMode.title }}</h2>
              <p>{{ currentMode.subtitle }}</p>
            </div>
            <span class="panel-tag">{{ currentMode.tag }}</span>
          </header>

          <el-form
            v-if="activeTab === 'simulate'"
            ref="simulationFormRef"
            :model="simulationForm"
            :rules="simulationRules"
            label-position="top"
            class="offer-form"
          >
            <div class="form-grid">
              <el-form-item label="目标公司" prop="companyName">
                <el-input v-model="simulationForm.companyName" maxlength="80" placeholder="例如：字节跳动" />
              </el-form-item>
              <el-form-item label="目标岗位" prop="jobTitle">
                <el-input v-model="simulationForm.jobTitle" maxlength="80" placeholder="例如：Java后端开发工程师" />
              </el-form-item>
              <el-form-item label="经验描述" prop="experienceYears">
                <el-input v-model="simulationForm.experienceYears" maxlength="40" placeholder="例如：5年" />
              </el-form-item>
              <el-form-item label="当前薪资" prop="currentSalary">
                <el-input v-model="simulationForm.currentSalary" maxlength="80" placeholder="例如：30万年包" />
              </el-form-item>
              <el-form-item label="期望薪资" prop="expectedSalary">
                <el-input v-model="simulationForm.expectedSalary" maxlength="80" placeholder="例如：45万年包" />
              </el-form-item>
              <el-form-item label="对方报价" prop="offerSalary">
                <el-input v-model="simulationForm.offerSalary" maxlength="80" placeholder="例如：38万年包" />
              </el-form-item>
            </div>
            <el-form-item label="候选人背景" prop="candidateBackground">
              <el-input
                v-model="simulationForm.candidateBackground"
                type="textarea"
                :rows="5"
                maxlength="1200"
                show-word-limit
                placeholder="写清楚项目影响、核心能力、管理/业务结果等谈薪筹码"
              />
            </el-form-item>
            <el-form-item label="HR 当前问题" prop="hrMessage">
              <el-input
                v-model="simulationForm.hrMessage"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="例如：你的期望薪资是多少？我们这个岗位预算有限。"
              />
            </el-form-item>
            <div class="action-row">
              <el-button type="primary" size="large" :loading="simulationLoading" @click="handleSimulationSubmit">
                生成模拟回复
              </el-button>
            </div>
          </el-form>

          <el-form
            v-else
            ref="scriptFormRef"
            :model="scriptForm"
            :rules="scriptRules"
            label-position="top"
            class="offer-form"
          >
            <div class="form-grid">
              <el-form-item label="目标公司" prop="companyName">
                <el-input v-model="scriptForm.companyName" maxlength="80" placeholder="例如：腾讯" />
              </el-form-item>
              <el-form-item label="目标岗位" prop="jobTitle">
                <el-input v-model="scriptForm.jobTitle" maxlength="80" placeholder="例如：高级产品经理" />
              </el-form-item>
              <el-form-item label="经验描述" prop="experienceYears">
                <el-input v-model="scriptForm.experienceYears" maxlength="40" placeholder="例如：6年" />
              </el-form-item>
              <el-form-item label="期望薪资" prop="expectedSalary">
                <el-input v-model="scriptForm.expectedSalary" maxlength="80" placeholder="例如：50万年包" />
              </el-form-item>
              <el-form-item label="当前报价" prop="offerSalary">
                <el-input v-model="scriptForm.offerSalary" maxlength="80" placeholder="例如：43万年包" />
              </el-form-item>
            </div>
            <el-form-item label="候选人背景" prop="candidateBackground">
              <el-input
                v-model="scriptForm.candidateBackground"
                type="textarea"
                :rows="5"
                maxlength="1200"
                show-word-limit
                placeholder="写清楚可支撑谈薪的经历、结果和稀缺性"
              />
            </el-form-item>
            <el-form-item label="谈判目标" prop="negotiationGoal">
              <el-input
                v-model="scriptForm.negotiationGoal"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="例如：争取年包到 50 万；如果现金空间有限，希望补签字费或明确调薪节点。"
              />
            </el-form-item>
            <div class="action-row">
              <el-button type="primary" size="large" :loading="scriptLoading" @click="handleScriptSubmit">
                生成话术模板
              </el-button>
            </div>
          </el-form>
        </section>

        <aside class="output-panel" aria-label="生成结果">
          <header class="panel-head compact">
            <div>
              <h2>生成结果</h2>
              <p>{{ resultSubtitle }}</p>
            </div>
          </header>

          <div v-if="currentLoading" class="result-state loading-state" aria-live="polite">
            <div class="skeleton-line wide"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-block"></div>
            <div class="skeleton-line short"></div>
          </div>

          <template v-else-if="activeTab === 'simulate' && simulationResult">
            <ResultBlock title="场景判断" :content="simulationResult.sceneSummary" tone="plain" />
            <ResultBlock title="建议回复" :content="simulationResult.candidateReply" tone="highlight" />
            <ResultBlock title="推进策略" :content="simulationResult.responseStrategy" tone="plain" />
            <ResultList title="风险提醒" :items="simulationResult.riskReminders" />
            <ResultList title="下一步行动" :items="simulationResult.nextActions" />
          </template>

          <template v-else-if="activeTab === 'script' && scriptResult">
            <ResultBlock title="开场确认" :content="scriptResult.openingScript" tone="highlight" />
            <ResultBlock title="争取报价" :content="scriptResult.counterOfferScript" tone="highlight" />
            <ResultBlock title="交换项话术" :content="scriptResult.benefitTradeoffScript" tone="plain" />
            <ResultBlock title="收口确认" :content="scriptResult.closingScript" tone="plain" />
            <ResultList title="使用提醒" :items="scriptResult.usageTips" />
          </template>

          <div v-else class="result-state empty-state">
            <div class="empty-mark">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path d="M8 7h8M8 11h5M8 15h7" />
                <rect x="4" y="3" width="16" height="18" rx="3" />
              </svg>
            </div>
            <h3>{{ currentMode.emptyTitle }}</h3>
            <p>{{ currentMode.emptyText }}</p>
          </div>
        </aside>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, defineComponent, h, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { generateSalaryScript, simulateSalaryNegotiation } from '@/api/offer'

const activeTab = ref('simulate')
const simulationFormRef = ref(null)
const scriptFormRef = ref(null)
const simulationLoading = ref(false)
const scriptLoading = ref(false)
const simulationResult = ref(null)
const scriptResult = ref(null)

const modeOptions = [
  {
    value: 'simulate',
    label: '薪资谈判模拟',
    desc: '针对 HR 当前问题生成回复',
    title: '谈判场景',
    subtitle: '填写当前报价、期望薪资和 HR 的原话，生成一段可直接回复的谈薪表达。',
    tag: '情景推演',
    emptyTitle: '等待谈判信息',
    emptyText: '补齐目标岗位、期望薪资、背景和 HR 问题后，右侧会生成建议回复。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M7 8h10M7 12h7M12 20l-3-3H6a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3h-2l-4 3Z"/></svg>'
  },
  {
    value: 'script',
    label: '谈薪话术模板',
    desc: '生成多段分场景话术',
    title: '话术素材',
    subtitle: '填写谈判目标和候选人筹码，生成开场、争取、交换项和收口四类话术。',
    tag: '模板生成',
    emptyTitle: '等待谈薪目标',
    emptyText: '填写背景、期望薪资和谈判目标后，右侧会生成分场景话术。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 3v4a2 2 0 0 0 2 2h4"/><path d="M6 21h12a2 2 0 0 0 2-2V8l-5-5H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2Z"/><path d="M8 13h8M8 17h5"/></svg>'
  }
]

const currentMode = computed(() => modeOptions.find((item) => item.value === activeTab.value) || modeOptions[0])
const currentLoading = computed(() => activeTab.value === 'simulate' ? simulationLoading.value : scriptLoading.value)
const resultSubtitle = computed(() => activeTab.value === 'simulate' ? '回复、策略和风险提醒会在这里整理。' : '话术会按使用顺序排列。')

// 表单只收集谈薪必要上下文，不要求用户填写实时行情数据。
const simulationForm = reactive({
  companyName: '',
  jobTitle: '',
  experienceYears: '',
  currentSalary: '',
  expectedSalary: '',
  offerSalary: '',
  candidateBackground: '',
  hrMessage: ''
})

const scriptForm = reactive({
  companyName: '',
  jobTitle: '',
  experienceYears: '',
  candidateBackground: '',
  expectedSalary: '',
  offerSalary: '',
  negotiationGoal: ''
})

const required = (message) => [{ required: true, message, trigger: 'blur' }]

const simulationRules = {
  jobTitle: required('请输入目标岗位'),
  expectedSalary: required('请输入期望薪资'),
  candidateBackground: required('请输入候选人背景'),
  hrMessage: required('请输入 HR 当前问题')
}

const scriptRules = {
  jobTitle: required('请输入目标岗位'),
  expectedSalary: required('请输入期望薪资'),
  candidateBackground: required('请输入候选人背景'),
  negotiationGoal: required('请输入谈判目标')
}

// 提交逻辑保持原有接口不变，只调整页面反馈和结果承载方式。
const handleSimulationSubmit = async () => {
  if (!simulationFormRef.value) return
  try {
    await simulationFormRef.value.validate()
  } catch {
    return
  }
  simulationLoading.value = true
  try {
    const res = await simulateSalaryNegotiation(simulationForm)
    simulationResult.value = res.data
    ElMessage.success('薪资谈判模拟已生成')
  } catch (err) {
    console.error('[Offer 辅助] 薪资谈判模拟生成失败:', err)
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    simulationLoading.value = false
  }
}

const handleScriptSubmit = async () => {
  if (!scriptFormRef.value) return
  try {
    await scriptFormRef.value.validate()
  } catch {
    return
  }
  scriptLoading.value = true
  try {
    const res = await generateSalaryScript(scriptForm)
    scriptResult.value = res.data
    ElMessage.success('谈薪话术模板已生成')
  } catch (err) {
    console.error('[Offer 辅助] 谈薪话术模板生成失败:', err)
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    scriptLoading.value = false
  }
}

const ResultBlock = defineComponent({
  props: {
    title: { type: String, required: true },
    content: { type: String, default: '' },
    tone: { type: String, default: 'plain' }
  },
  setup(props) {
    // 结果块统一用标题栏和正文底色分层，避免某一类结果单独呈现为气泡。
    return () => h('article', { class: ['result-block', props.tone] }, [
      h('div', { class: 'result-head' }, [
        h('span', { class: 'result-title' }, props.title)
      ]),
      h('div', { class: 'result-body' }, [
        h('p', { class: 'result-text' }, props.content || '暂无')
      ])
    ])
  }
})

const ResultList = defineComponent({
  props: {
    title: { type: String, required: true },
    items: { type: Array, default: () => [] }
  },
  setup(props) {
    return () => h('article', { class: 'result-block plain' }, [
      h('div', { class: 'result-head' }, [
        h('span', { class: 'result-title' }, props.title)
      ]),
      props.items.length > 0
        ? h('div', { class: 'result-body' }, [
          h('ol', { class: 'result-list' }, props.items.map((item, index) => h('li', { class: 'result-list-item' }, [
            h('span', { class: 'list-index' }, `${index + 1}、`),
            h('span', { class: 'list-text' }, item)
          ])))
        ])
        : h('div', { class: 'result-body' }, [
          h('p', { class: 'result-text muted' }, '暂无')
        ])
    ])
  }
})
</script>

<style scoped>
.offer-page {
  --offer-accent: var(--orange-main);
  --offer-accent-deep: var(--orange-deep);
  --offer-soft: var(--orange-light-bg);
  width: min(100%, 1280px);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 100%;
}

.offer-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 8px 2px 4px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 720px;
}

.eyebrow {
  width: fit-content;
  font-size: 12px;
  font-weight: 700;
  color: var(--offer-accent-deep);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.offer-hero h1 {
  margin: 0;
  color: var(--text-title);
  font-size: 30px;
  line-height: 1.15;
  font-weight: 750;
  text-wrap: balance;
}

.offer-hero p,
.hero-note,
.panel-head p,
.mode-button small {
  color: var(--text-muted);
  text-wrap: pretty;
}

.offer-hero p {
  margin: 0;
  max-width: 58ch;
  font-size: 15px;
}

.hero-note {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 220px;
  padding: 14px 16px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: color-mix(in srgb, var(--bg-card) 88%, var(--offer-soft));
  font-size: 13px;
}

.note-label {
  color: var(--text-title);
  font-weight: 650;
}

.offer-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mode-button {
  min-height: 86px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: var(--bg-card);
  color: var(--text-body);
  cursor: pointer;
  text-align: left;
  transition: transform 160ms ease-out, border-color 160ms ease-out, box-shadow 160ms ease-out, background-color 160ms ease-out;
}

.mode-button:hover {
  transform: translateY(-1px);
  border-color: var(--orange-border);
  box-shadow: var(--shadow-card);
}

.mode-button:active {
  transform: translateY(0);
}

.mode-button:focus-visible {
  outline: 2px solid var(--offer-accent);
  outline-offset: 2px;
}

.mode-button.active {
  border-color: var(--offer-accent);
  background: color-mix(in srgb, var(--bg-card) 82%, var(--offer-soft));
}

.mode-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--offer-accent-deep);
  background: var(--offer-soft);
  flex: 0 0 auto;
}

.mode-icon :deep(svg) {
  width: 22px;
  height: 22px;
}

.mode-button strong,
.mode-button small {
  display: block;
}

.mode-button strong {
  margin-bottom: 4px;
  font-size: 15px;
  color: var(--text-title);
}

.mode-button small {
  font-size: 13px;
  line-height: 1.45;
}

.workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.72fr);
  gap: 18px;
  align-items: start;
}

.input-panel,
.output-panel {
  min-width: 0;
  border: 1px solid var(--border-card);
  border-radius: 14px;
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.input-panel {
  padding: 22px;
}

.output-panel {
  padding: 22px 24px;
  position: sticky;
  top: 84px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head.compact {
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0 0 6px;
  color: var(--text-title);
  font-size: 18px;
  line-height: 1.25;
  font-weight: 700;
}

.panel-head p {
  margin: 0;
  max-width: 62ch;
  font-size: 13px;
  line-height: 1.6;
}

.panel-tag {
  flex: 0 0 auto;
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--offer-soft);
  color: var(--offer-accent-deep);
  font-size: 12px;
  font-weight: 650;
}

.offer-form {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 14px;
}

.offer-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.offer-form :deep(.el-form-item__label) {
  margin-bottom: 6px;
  color: var(--text-title);
  font-weight: 600;
}

.offer-form :deep(.el-input__wrapper),
.offer-form :deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--border-input) inset;
  background: var(--bg-input);
  transition: box-shadow 160ms ease-out, background-color 160ms ease-out;
}

.offer-form :deep(.el-input__wrapper:hover),
.offer-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px var(--orange-border) inset;
}

.offer-form :deep(.el-input__wrapper.is-focus),
.offer-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--offer-accent) inset, 0 0 0 3px color-mix(in srgb, var(--offer-accent) 18%, transparent);
}

.action-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
}

.action-row :deep(.el-button) {
  min-width: 156px;
  border-radius: 10px;
  font-weight: 650;
}

.result-state {
  min-height: 360px;
  border: 1px solid var(--border-divider);
  border-radius: 12px;
  background: color-mix(in srgb, var(--bg-card) 92%, var(--bg-elevated));
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 12px;
  padding: 48px 24px;
}

.empty-mark {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--offer-soft) 60%, var(--bg-card));
  color: var(--offer-accent-deep);
}

.empty-mark svg {
  width: 26px;
  height: 26px;
}

.empty-state h3 {
  margin: 4px 0 0;
  color: var(--text-title);
  font-size: 16px;
  font-weight: 650;
}

.empty-state p {
  margin: 0;
  max-width: 30ch;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.loading-state {
  padding: 20px 22px;
}

.skeleton-line,
.skeleton-block {
  border-radius: 8px;
  background: linear-gradient(90deg, color-mix(in srgb, var(--bg-elevated) 80%, var(--bg-card)), color-mix(in srgb, var(--bg-card-hover) 90%, var(--bg-elevated)), color-mix(in srgb, var(--bg-elevated) 80%, var(--bg-card)));
  background-size: 200% 100%;
  animation: shimmer 1.4s cubic-bezier(0.16, 1, 0.3, 1) infinite;
}

.skeleton-line {
  width: 72%;
  height: 13px;
  margin-bottom: 14px;
}

.skeleton-line.wide {
  width: 92%;
}

.skeleton-line.short {
  width: 48%;
}

.skeleton-block {
  height: 110px;
  margin: 16px 0;
}

.result-block {
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--border-divider) 86%, transparent);
  border-radius: 12px;
  background: color-mix(in srgb, var(--bg-elevated) 58%, var(--bg-card));
}

.result-block:first-child {
  margin-top: 0;
}

.result-block:last-child {
  margin-bottom: 0;
}

.result-block.highlight {
  border: 1px solid var(--orange-border);
  background: color-mix(in srgb, var(--offer-soft) 44%, var(--bg-card));
  box-shadow: 0 1px 3px color-mix(in srgb, var(--offer-accent) 6%, transparent);
}

.result-head {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.result-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  width: fit-content;
  max-width: 100%;
  padding: 5px 9px;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--border-divider) 78%, transparent);
  background: color-mix(in srgb, var(--bg-card) 82%, var(--bg-elevated));
  color: var(--text-title);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.35;
}

.result-title::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--offer-accent);
  flex: 0 0 auto;
}

.result-block.highlight .result-title {
  border-color: color-mix(in srgb, var(--offer-accent) 28%, transparent);
  background: color-mix(in srgb, var(--offer-soft) 72%, var(--bg-card));
  color: var(--offer-accent-deep);
}

.result-body {
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--border-divider) 54%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--bg-card) 90%, white);
}

.result-block.highlight .result-body {
  border-color: color-mix(in srgb, var(--orange-border) 62%, transparent);
  background: color-mix(in srgb, var(--bg-card) 82%, white);
}

.result-text {
  margin: 0;
  color: var(--text-body);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  text-wrap: pretty;
}

.result-text.muted {
  color: var(--text-muted);
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.result-list-item {
  display: flex;
  gap: 4px;
  align-items: baseline;
  padding: 8px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--border-divider) 70%, transparent);
  color: var(--text-body);
  font-size: 14px;
  line-height: 1.65;
}

.result-list-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.result-list-item:first-child {
  padding-top: 0;
}

.list-index {
  flex-shrink: 0;
  color: var(--offer-accent-deep);
  font-size: 14px;
  font-weight: 750;
  line-height: 1.65;
  margin-top: 0;
}

@keyframes shimmer {
  0% {
    background-position: 120% 0;
  }
  100% {
    background-position: -80% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mode-button,
  .offer-form :deep(.el-input__wrapper),
  .offer-form :deep(.el-textarea__inner) {
    transition: none;
  }

  .skeleton-line,
  .skeleton-block {
    animation: none;
  }
}

@media (max-width: 1100px) {
  .workbench {
    grid-template-columns: 1fr;
  }

  .output-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .offer-page {
    gap: 16px;
  }

  .offer-hero,
  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-note {
    min-width: 0;
  }

  .mode-switch {
    grid-template-columns: 1fr;
  }

  .input-panel,
  .output-panel {
    padding: 16px;
    border-radius: 12px;
  }

  .result-block,
  .result-block.highlight {
    padding: 14px 16px;
    border-radius: 12px;
  }

  .result-list-item {
    gap: 8px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .action-row {
    justify-content: stretch;
  }

  .action-row :deep(.el-button) {
    width: 100%;
    min-height: 44px;
  }
}
</style>
