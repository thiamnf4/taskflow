package com.taskflow.service;

import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.model.Tenant;
import com.taskflow.model.User;
import com.taskflow.repository.TenantRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private TenantRepository tenantRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @InjectMocks
    private AuthService authService;
    
    private RegisterRequest registerRequest;
    private Tenant tenant;
    private User user;
    
    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setOrganizationName("Test Org");
        registerRequest.setSubdomain("testorg");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Org");
        tenant.setSubdomain("testorg");
        
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setTenant(tenant);
    }
    
    @Test
    void testRegister_Success() {
        // Given
        when(tenantRepository.existsBySubdomain(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(userRepository.existsByEmailAndTenantId(anyString(), anyLong())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(any(), anyLong())).thenReturn("test_token");
        
        // When
        AuthResponse response = authService.register(registerRequest);
        
        // Then
        assertNotNull(response);
        assertEquals("test_token", response.getToken());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(user.getEmail(), response.getEmail());
        
        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testRegister_SubdomainExists() {
        // Given
        when(tenantRepository.existsBySubdomain(anyString())).thenReturn(true);
        
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });
        
        assertTrue(exception.getMessage().contains("Subdomain already exists"));
        verify(tenantRepository, never()).save(any());
    }
}
