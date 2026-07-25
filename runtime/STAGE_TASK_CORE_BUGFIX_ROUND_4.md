# TASK_CORE_BUGFIX_ROUND_4_REPORT_ASYNC_STATUS_FRONTEND_ADAPT - Stage Document

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_4_REPORT_ASYNC_STATUS_FRONTEND_ADAPT
- Status: Completed, waiting for acceptance
- Date: 2026-04-24

## Problem Summary
- Frontend uses old sync-report logic, doesn't adapt to async report generation
- After ending session, shows "completed" instantly but report page is empty
- Missing intermediate state: "session ended but report generating"

## Root Causes Identified
1. InterviewSessionView: End confirmation directly shows "view report" without distinguishing
2. InterviewReportView: Missing "generating" state UI
3. InterviewHistoryView: Only has status (0/1), no report generation status

## Fixes Applied

### Fix 1: InterviewReportView.vue (Primary)
- Added `isReportGenerating` computed property
- Added "generating" state UI with animated progress bar
- Added dynamic description text based on polling rounds

### Fix 2: InterviewSessionView.vue
- Changed end confirmation message to indicate "report generating..."

### Fix 3: InterviewHistoryView.vue
- Added `reportStatus` field support (for future backend adaptation)
- Distinguished display between "ended but report not ready" and "ended + report ready"

## Verification
- Frontend build: ✅ PASSED (`npm.cmd run build`)

## Files Modified
1. `frontend/app/src/views/interview/InterviewReportView.vue`
2. `frontend/app/src/views/interview/InterviewSessionView.vue`
3. `frontend/app/src/views/interview/InterviewHistoryView.vue`

## Acceptance Criteria
1. After ending session, page shows "report generating..." instead of "completed"
2. Report generating state shows clear UI with progress bar
3. Report completes, page auto-refreshes to final result
4. History page correctly shows status for unready reports

## Next Steps
- Wait for manual acceptance
- If backend needs to add `reportStatus` field, will do in next round
- After acceptance, can proceed to next round