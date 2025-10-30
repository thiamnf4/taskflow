package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.model.Project;
import com.taskflow.model.Tenant;
import com.taskflow.model.User;
import com.taskflow.repository.AuditLogRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.util.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    
    @Mock
    private ProjectRepository projectRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private ProjectService projectService;
    
    private Tenant tenant;
    private User user;
    private Project project;
    
    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");
        
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setTenant(tenant);
        
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("Test Description");
        project.setOwner(user);
        project.setTenant(tenant);
        
        TenantContext.setCurrentTenant(1L);
        
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testGetAllProjects() {
        // Given
        List<Project> projects = Arrays.asList(project);
        when(projectRepository.findAllByTenantId(anyLong())).thenReturn(projects);
        
        // When
        List<Project> result = projectService.getAllProjects();
        
        // Then
        assertEquals(1, result.size());
        assertEquals("Test Project", result.get(0).getName());
        verify(projectRepository).findAllByTenantId(1L);
    }
    
    @Test
    void testGetProjectById() {
        // Given
        when(projectRepository.findByIdAndTenantId(anyLong(), anyLong())).thenReturn(Optional.of(project));
        
        // When
        Project result = projectService.getProjectById(1L);
        
        // Then
        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        verify(projectRepository).findByIdAndTenantId(1L, 1L);
    }
    
    @Test
    void testCreateProject() {
        // Given
        ProjectRequest request = new ProjectRequest();
        request.setName("New Project");
        request.setDescription("New Description");
        
        when(userRepository.findByEmailAndTenantId(anyString(), anyLong())).thenReturn(Optional.of(user));
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        
        // When
        Project result = projectService.createProject(request);
        
        // Then
        assertNotNull(result);
        verify(projectRepository).save(any(Project.class));
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void testDeleteProject() {
        // Given
        when(projectRepository.findByIdAndTenantId(anyLong(), anyLong())).thenReturn(Optional.of(project));
        doNothing().when(projectRepository).delete(any(Project.class));
        
        // When
        projectService.deleteProject(1L);
        
        // Then
        verify(projectRepository).delete(project);
        verify(auditLogRepository).save(any());
    }
}
