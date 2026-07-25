# TASK_CORE_BUGFIX_ROUND_3_AI_ENGINE_RUNTIME_FORBIDDEN - Stage Document

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_3_AI_ENGINE_RUNTIME_FORBIDDEN
- Status: Completed, waiting for acceptance
- Date: 2026-04-24

## Problem Summary
- AI engine switched to new provider (SiliconFlow), but runtime request returns 403 Forbidden
- Root cause: runtimeApiKey lacks fallback handling, empty value from database overwrites valid key to null
- Additional: Possible masked key ("sk-****abcd") being written back to database

## Root Causes Identified
1. **InterviewAiServiceImpl.resolveRuntimeConfig()**: `runtimeApiKey` lacked fallback; when database returns null, it overwrites valid key to null → 403
2. **AdminController**: No validation to prevent masked key from being submitted and saved
3. **Missing validation**: No minimum usable config validation in runtime

## Fixes Applied

### Fix 1: InterviewAiServiceImpl (Lines ~1267-1351)
- Added fallback logic: when database apiKey is empty, keep using local fallback instead of null
- Added final fallback: try to get from environment variables
- Added validation: throw clear error when all fallbacks fail

### Fix 2: ResumeAiServiceImpl (Lines ~364-439)
- Aligned with InterviewAiServiceImpl logic
- Same fallback and validation mechanism

### Fix 3: AdminController (Lines ~266-272, ~1003-1027)
- Added `isMaskedApiKey()` validation method
- Reject API key that contains "****" and length <= 20
- Prevent masked value from overwriting real key

## Verification
- Backend compilation: ✅ PASSED (`mvn.cmd -q -DskipTests compile`)

## Files Modified
1. `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
2. `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
3. `server/src/main/java/com/airesume/server/controller/AdminController.java`

## Acceptance Criteria
1. Database activation config can be correctly read and used for requests
2. Empty apiKey in database won't cause 403 due to null key
3. Masked value ("sk-****abcd") cannot be saved to database
4. Clear error thrown when no valid config available

## Next Steps
- Wait for manual acceptance
- After acceptance, can proceed to next round