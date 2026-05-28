package com.airesume.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 校验大字段默认不参与 MyBatis-Plus 全列查询，避免列表和轮询接口加载长文本。
 */
class EntityLargeFieldSelectTest {

    @Test
    void shouldDisableDefaultSelectForResumeDiagnosisLargeFields() throws Exception {
        assertSelectFalse(ResumeDiagnosisTask.class, "resumeText");
        assertSelectFalse(ResumeDiagnosisTask.class, "diagnosisResult");
    }

    @Test
    void shouldDisableDefaultSelectForResumeDerivativeLargeFields() throws Exception {
        assertSelectFalse(ResumeJobMatchRecord.class, "resumeText");
        assertSelectFalse(ResumeJobMatchRecord.class, "jdText");
        assertSelectFalse(ResumePolishRecord.class, "sourceResumeText");
        assertSelectFalse(ResumePolishRecord.class, "jdText");
        assertSelectFalse(ResumePolishRecord.class, "polishedResumeText");
        assertSelectFalse(ResumePolishRecord.class, "documentJson");
    }

    @Test
    void shouldDisableDefaultSelectForInterviewLargeFields() throws Exception {
        assertSelectFalse(InterviewSession.class, "evaluationReport");
        assertSelectFalse(MockInterviewJobTargetRecord.class, "jdText");
        assertSelectFalse(MockInterviewJobTargetRecord.class, "generatedQuestions");
        assertSelectFalse(MockInterviewJobTargetRecord.class, "jobTargetedFeedback");
    }

    private void assertSelectFalse(Class<?> entityClass, String fieldName) throws Exception {
        Field field = entityClass.getDeclaredField(fieldName);
        TableField tableField = field.getAnnotation(TableField.class);
        assertFalse(tableField.select(), entityClass.getSimpleName() + "." + fieldName + " should use select=false");
    }
}
