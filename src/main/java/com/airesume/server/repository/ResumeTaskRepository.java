package com.airesume.server.repository;

import com.airesume.server.entity.ResumeDiagnosisTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 简历任务仓储
 */
@Repository
public interface ResumeTaskRepository extends JpaRepository<ResumeDiagnosisTask, Long> {


    /**
     * 根据用户ID查询所有任务（不分页）
     */
    List<ResumeDiagnosisTask> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 根据用户ID分页查询任务
     */
    Page<ResumeDiagnosisTask> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID和状态查询
     */
    List<ResumeDiagnosisTask> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);

    /**
     * 根据用户ID统计任务数量
     */
    long countByUserId(Long userId);

    /**
     * 根据用户ID和状态统计任务数量
     */
    long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 查询指定时间之前创建的、状态为处理中的任务
     */
    @Query("SELECT t FROM ResumeDiagnosisTask t WHERE t.status = 1 AND t.updateTime < :timeoutTime")
    List<ResumeDiagnosisTask> findTimeoutProcessingTasks(@Param("timeoutTime") LocalDateTime timeoutTime);

    /**
     * 更新任务状态
     */
    @Modifying
    @Query("UPDATE ResumeDiagnosisTask t SET t.status = :status, t.updateTime = :updateTime WHERE t.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新任务结果
     */
    @Modifying
    @Query("UPDATE ResumeDiagnosisTask t SET t.diagnosisResult = :diagnosisResult, t.status = :status, t.updateTime = :updateTime WHERE t.id = :id")
    int updateResult(@Param("id") Long id, @Param("diagnosisResult") String diagnosisResult, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新错误信息
     */
    @Modifying
    @Query("UPDATE ResumeDiagnosisTask t SET t.errorMsg = :errorMsg, t.status = :status, t.updateTime = :updateTime WHERE t.id = :id")
    int updateError(@Param("id") Long id, @Param("errorMsg") String errorMsg, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);
}
