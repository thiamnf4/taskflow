package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.AuditLog;
import com.taskflow.model.Project;
import com.taskflow.model.User;
import com.taskflow.repository.AuditLogRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    
    @Cacheable(value = "projects", key = "#tenantId")
    public List<Project> getAllProjects() {
        Long tenantId = TenantContext.getCurrentTenant();
        return projectRepository.findAllByTenantId(tenantId);
    }
    
    @Cacheable(value = "projects", key = "#tenantId + '_' + #projectId")
    public Project getProjectById(Long projectId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
    
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public Project createProject(ProjectRequest request) {
        Long tenantId = TenantContext.getCurrentTenant();
        String userEmail = getCurrentUserEmail();
        
        User owner = userRepository.findByEmailAndTenantId(userEmail, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(owner);
        project.setTenant(owner.getTenant());
        
        project = projectRepository.save(project);
        
        // Log action
        logAction("CREATE_PROJECT", "Project", project.getId());
        
        return project;
    }
    
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public Project updateProject(Long projectId, ProjectRequest request) {
        Project project = getProjectById(projectId);
        
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        
        project = projectRepository.save(project);
        
        // Log action
        logAction("UPDATE_PROJECT", "Project", project.getId());
        
        return project;
    }
    
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public void deleteProject(Long projectId) {
        Project project = getProjectById(projectId);
        projectRepository.delete(project);
        
        // Log action
        logAction("DELETE_PROJECT", "Project", projectId);
    }
    
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
    
    private void logAction(String action, String resourceType, Long resourceId) {
        Long tenantId = TenantContext.getCurrentTenant();
        String userEmail = getCurrentUserEmail();
        
        User user = userRepository.findByEmailAndTenantId(userEmail, tenantId).orElse(null);
        
        AuditLog log = new AuditLog();
        log.setTenant(user != null ? user.getTenant() : null);
        log.setUser(user);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        
        auditLogRepository.save(log);
    }
}
