package com.taskflow.service;

import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.model.Tenant;
import com.taskflow.model.User;
import com.taskflow.repository.TenantRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtUtil;
import com.taskflow.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if subdomain already exists
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new RuntimeException("Subdomain already exists");
        }
        
        // Create tenant
        Tenant tenant = new Tenant();
        tenant.setName(request.getOrganizationName());
        tenant.setSubdomain(request.getSubdomain());
        tenant = tenantRepository.save(tenant);
        
        // Check if user already exists for this tenant
        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenant.getId())) {
            throw new RuntimeException("User already exists");
        }
        
        // Create owner user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setTenant(tenant);
        user.setRoles(Set.of(User.Role.OWNER));
        user = userRepository.save(user);
        
        // Generate JWT token
        TenantContext.setCurrentTenant(tenant.getId());
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, tenant.getId());
        
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                tenant.getId(),
                tenant.getName()
        );
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find tenant
        Tenant tenant = tenantRepository.findBySubdomain(request.getSubdomain())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        // Set tenant context for authentication
        TenantContext.setCurrentTenant(tenant.getId());
        
        // Find user
        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenant.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, tenant.getId());
        
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                tenant.getId(),
                tenant.getName()
        );
    }
}
