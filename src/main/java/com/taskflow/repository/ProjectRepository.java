package com.taskflow.repository;

import com.taskflow.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId")
    List<Project> findAllByTenantId(Long tenantId);
    
    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.tenant.id = :tenantId")
    Optional<Project> findByIdAndTenantId(Long projectId, Long tenantId);
    
    @Query("SELECT p FROM Project p WHERE p.owner.id = :userId AND p.tenant.id = :tenantId")
    List<Project> findByOwnerIdAndTenantId(Long userId, Long tenantId);
}
