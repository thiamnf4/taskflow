package com.taskflow.repository;

import com.taskflow.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.project.tenant.id = :tenantId")
    List<Task> findByProjectIdAndTenantId(Long projectId, Long tenantId);
    
    @Query("SELECT t FROM Task t WHERE t.id = :taskId AND t.project.tenant.id = :tenantId")
    Optional<Task> findByIdAndTenantId(Long taskId, Long tenantId);
    
    @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.project.tenant.id = :tenantId")
    List<Task> findByAssigneeIdAndTenantId(Long userId, Long tenantId);
}
