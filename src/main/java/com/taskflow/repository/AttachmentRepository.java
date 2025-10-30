package com.taskflow.repository;

import com.taskflow.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    
    @Query("SELECT a FROM Attachment a WHERE a.task.id = :taskId AND a.task.project.tenant.id = :tenantId")
    List<Attachment> findByTaskIdAndTenantId(Long taskId, Long tenantId);
}
