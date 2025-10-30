package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.model.AuditLog;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.AuditLogRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Cacheable(value = "tasks", key = "#tenantId + '_' + #projectId")
    public List<Task> getTasksByProject(Long projectId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return taskRepository.findByProjectIdAndTenantId(projectId, tenantId);
    }
    
    @Cacheable(value = "tasks", key = "#tenantId + '_task_' + #taskId")
    public Task getTaskById(Long taskId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }
    
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public Task createTask(Long projectId, TaskRequest request) {
        Long tenantId = TenantContext.getCurrentTenant();
        
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        Task task = new Task();
        task.setProject(project);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : Task.Status.TODO);
        task.setPriority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM);
        task.setDueDate(request.getDueDate());
        
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findByIdAndTenantId(request.getAssigneeId(), tenantId)
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }
        
        task = taskRepository.save(task);
        
        // Send real-time update via WebSocket
        sendTaskUpdate(projectId, task);
        
        // Send notification asynchronously
        if (task.getAssignee() != null) {
            sendNotificationAsync(task.getAssignee().getEmail(), "New task assigned: " + task.getTitle());
        }
        
        // Log action
        logAction("CREATE_TASK", "Task", task.getId());
        
        return task;
    }
    
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public Task updateTask(Long taskId, TaskRequest request) {
        Task task = getTaskById(taskId);
        Long tenantId = TenantContext.getCurrentTenant();
        
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findByIdAndTenantId(request.getAssigneeId(), tenantId)
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }
        
        task = taskRepository.save(task);
        
        // Send real-time update
        sendTaskUpdate(task.getProject().getId(), task);
        
        // Log action
        logAction("UPDATE_TASK", "Task", task.getId());
        
        return task;
    }
    
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public void deleteTask(Long taskId) {
        Task task = getTaskById(taskId);
        Long projectId = task.getProject().getId();
        
        taskRepository.delete(task);
        
        // Send real-time update
        messagingTemplate.convertAndSend("/topic/project/" + projectId + "/tasks", 
                "TASK_DELETED:" + taskId);
        
        // Log action
        logAction("DELETE_TASK", "Task", taskId);
    }
    
    private void sendTaskUpdate(Long projectId, Task task) {
        messagingTemplate.convertAndSend("/topic/project/" + projectId + "/tasks", task);
    }
    
    @Async
    public void sendNotificationAsync(String email, String message) {
        System.out.println("Sending notification to " + email + ": " + message);
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
