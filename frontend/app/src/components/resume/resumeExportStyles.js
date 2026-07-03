/**
 * 简历导出 CSS —— 内联到 HTML 中供 Chrome --print-to-pdf 渲染。
 * 选择器均已去掉 Vue scoped 属性，配合 buildExportElement 克隆后的 DOM 使用。
 */

const resumeExportCss = `
/* ===== 基础重置 ===== */
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
body{margin:0;padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact}

/* ===== 简历根容器 ===== */
.resume-template{
  --resume-accent:#1b5b57;
  --resume-accent-soft:#e8f0ee;
  --resume-gold:#b18757;
  --resume-text:#1f2933;
  --resume-muted:#52606d;
  --resume-line:#d6ddd8;
  width:100%;
  box-sizing:border-box;
  font-family:"PingFang SC","Hiragino Sans GB","Microsoft YaHei","Noto Sans SC",Arial,sans-serif;
}

.resume-template--print{width:190mm;margin:0 auto}

/* ===== 纸张 ===== */
.resume-paper{
  box-sizing:border-box;
  background:#fff;
  color:var(--resume-text);
}

.resume-template--print .resume-paper{
  padding:8mm 10mm 8mm;
  border:none;
  box-shadow:none;
}

/* ===== 主体 ===== */
.resume-main{margin-top:10px}

.resume-section+.resume-section{margin-top:20px}

.resume-section-head{
  display:flex;
  align-items:center;
  gap:14px;
  margin-bottom:14px;
}

/* ===== 分区标题栏 ===== */
.section-tab{
  display:inline-flex;
  align-items:center;
  gap:10px;
  min-width:0;
  padding:5px 14px 5px 10px;
  background:linear-gradient(90deg,rgba(27,91,87,0.14),rgba(27,91,87,0.05));
  border-left:3px solid var(--resume-gold);
  border-radius:0 16px 16px 0;
}

.section-tab-mark{
  width:8px;height:8px;border-radius:999px;
  background:var(--resume-gold);flex-shrink:0;
}

.section-title-input{
  min-width:80px;margin:0;font-size:17px;line-height:1.2;
  font-weight:700;color:var(--resume-accent);letter-spacing:0.08em;white-space:nowrap;
}

.section-line{
  flex:1;min-width:0;height:1px;
  background:linear-gradient(90deg,rgba(177,135,87,0.55),rgba(214,221,216,0.65));
}

/* ===== 个人信息区 ===== */
.profile-card{
  display:grid;
  grid-template-columns:minmax(0,1fr) 118px;
  gap:16px;align-items:start;
  padding:4px 0 6px;
}
.profile-main{min-width:0}
.profile-name-row{display:flex;align-items:baseline;gap:14px;flex-wrap:wrap}

.profile-name-input{
  display:block;min-width:0;margin:0;
  font-size:38px;line-height:1.06;font-weight:800;
  color:var(--resume-text,#143f45);letter-spacing:0.06em;
}

.profile-target-input{
  display:flex;align-items:center;
  min-height:28px;padding:4px 18px 4px 28px;
  background:rgba(177,135,87,0.16);color:#7a5631;
  font-size:14px;font-weight:700;line-height:1.35;
  border-radius:999px;
}

.profile-meta-grid{
  display:grid;grid-template-columns:repeat(2,minmax(0,1fr));
  gap:8px 18px;margin-top:14px;
}
.profile-meta-item--wide{grid-column:1/-1}

.profile-meta-card{position:relative;display:flex;align-items:center;min-width:0}

.profile-meta-input{
  width:100%;min-width:0;font-size:14px;line-height:1.72;
  color:var(--resume-muted);word-break:break-word;
}

.profile-summary{
  margin-top:12px;padding:10px 12px;
  border-left:2px solid rgba(27,91,87,0.22);
  background:rgba(232,240,238,0.32);
}
.profile-summary-item{position:relative}
.profile-summary-item+.profile-summary-item{margin-top:10px}
.profile-summary-input{
  min-height:44px;font-size:14px;line-height:1.8;
  color:#334155;white-space:pre-wrap;
}

/* ===== 照片区（诊断结果页 ResumeTemplate） ===== */
.profile-photo{
  position:relative;justify-self:end;width:118px;
  display:flex;flex-direction:column;align-items:center;gap:8px;
}

/* ===== 模板编辑器头部布局 ===== */
.resume-header{
  display:flex;align-items:flex-start;gap:16px;
}
.header-left{
  flex:1;min-width:0;
}
.name{
  width:100%;
}
.header-photo{
  flex-shrink:0;width:90px;height:112px;
  border-radius:4px;overflow:hidden;
  background:#f0f0f0;
  display:flex;align-items:center;justify-content:center;
}
.header-photo img{
  width:100%;height:100%;object-fit:cover;display:block;
}
.photo-placeholder{
  font-size:28px;opacity:0.3;
}
.photo-frame{
  display:block;width:118px;height:146px;box-sizing:border-box;
  border:none;
  background:var(--bg-elevated,#f3f6f5);
  overflow:hidden;
}
.photo-image{display:block;width:100%;height:100%;object-fit:cover}
.photo-placeholder{
  display:flex;align-items:center;justify-content:center;
  width:100%;height:100%;padding:14px;font-size:12px;line-height:1.5;
  color:#6b7280;text-align:center;
  background:var(--bg-elevated,#f3f6f5);
}

/* ===== 条目行（教育/工作/项目） ===== */
.entry-row{
  display:grid;
  grid-template-columns:minmax(0,1.5fr) minmax(0,1fr) minmax(118px,0.85fr);
  gap:16px;align-items:baseline;
}
.entry-cell-input,.entry-cell--left,.entry-cell--middle,.entry-cell--right{
  min-width:0;font-size:14px;line-height:1.72;
  color:var(--resume-text);word-break:break-word;
}
.entry-cell--left{font-weight:700}
.entry-cell--middle{font-weight:600;color:#334155}
.entry-cell--right{text-align:right;color:#5b6774}

/* ===== 文本行 ===== */
.label-line,.text-line,.subsection-line{
  margin:0;font-size:14px;line-height:1.82;
  color:#24323f;white-space:pre-wrap;word-break:break-word;
}
.subsection-line{font-size:15px;font-weight:700;color:#173a52}
.text-line--heading{font-size:15px;font-weight:700;color:#173a52}
.text-line--bullet{position:relative;padding-left:14px}
.text-line--bullet::before{
  content:'•';position:absolute;left:0;top:0;color:var(--resume-gold);
}

.label-key-input{display:inline-block;width:auto;margin-right:6px;font-weight:700;color:#253542}
.label-value-input{color:#425466}

/* ===== 区块内容 ===== */
.resume-section-body{display:flex;flex-direction:column;gap:6px}
.resume-block-shell{position:relative}
.resume-block{position:relative;min-width:0}

/* ===== 导出静态字段 ===== */
.export-static-field{
  width:100%;box-sizing:border-box;
  white-space:pre-wrap;word-break:break-word;
}

/* ===== print 模式下表单元素透明化 ===== */
.resume-template--print .resume-inline-input,
.resume-template--print .resume-textarea-input,
.resume-template--print .section-title-input{
  background:transparent!important;
  box-shadow:none!important;
  pointer-events:none;
  caret-color:transparent;
  resize:none;
  appearance:none;
  -webkit-appearance:none;
}
.resume-template--print .profile-meta-card,
.resume-template--print .profile-summary-item{
  background:transparent;box-shadow:none;
}
`

export default resumeExportCss
